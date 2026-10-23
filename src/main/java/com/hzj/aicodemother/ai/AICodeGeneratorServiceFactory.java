package com.hzj.aicodemother.ai;

import com.hzj.aicodemother.model.entity.ChatHistory;
import com.hzj.aicodemother.ai.tool.FileWriteTool;
import com.hzj.aicodemother.service.ChatHistoryService;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import dev.langchain4j.community.store.memory.chat.redis.RedisChatMemoryStore;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.service.AiServices;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;

@Component
@Slf4j
public class AICodeGeneratorServiceFactory {

    private static final int MEMORY_MAX_MESSAGES = 10;
    // One file write usually adds an AI tool request and a tool result. Keep the current
    // user request in memory throughout the bounded Vue generation (24 tool rounds).
    private static final int VUE_MEMORY_MAX_MESSAGES = 64;
    private static final int VUE_MAX_TOOL_ROUND_TRIPS = 24;
    private static final long SERVICE_CACHE_MAXIMUM_SIZE = 500;
    private static final Duration SERVICE_CACHE_IDLE_TTL = Duration.ofMinutes(30);

    @Resource
    private ChatModel qwenChatModel;
    @Resource
    private StreamingChatModel qwenStreamingChatModel;
    @Resource
    private RedisChatMemoryStore redisChatMemoryStore;
    @Resource
    private ChatHistoryService chatHistoryService;

    private final Cache<Long, AppAiServices> serviceCache = Caffeine.newBuilder()
            .maximumSize(SERVICE_CACHE_MAXIMUM_SIZE)
            .expireAfterAccess(SERVICE_CACHE_IDLE_TTL)
            .build();

    public AICodeGeneratorService getAICodeGeneratorService(Long appId) {
        if (appId == null || appId <= 0) {
            throw new IllegalArgumentException("appId must be positive");
        }
        //生成appid对应的AICodeGeneratorService实例
        return serviceCache.get(appId, this::createAppAiServices).plain();
    }

    public VueCodeGeneratorService getVueCodeGeneratorService(Long appId) {
        if (appId == null || appId <= 0) {
            throw new IllegalArgumentException("appId must be positive");
        }
        return serviceCache.get(appId, this::createAppAiServices).vue();
    }

    private AppAiServices createAppAiServices(Long appId) {
        String memoryId = memoryId(appId);
        List<ChatMessage> storedMessages = redisChatMemoryStore.getMessages(memoryId);
        if (storedMessages.isEmpty()) {
            List<ChatMessage> historyMessages = chatHistoryService
                    .listLatestForMemory(appId, MEMORY_MAX_MESSAGES)
                    .stream()
                    .map(this::toChatMessage)
                    .filter(message -> message != null)
                    .toList();
            if (!historyMessages.isEmpty()) {
                redisChatMemoryStore.updateMessages(memoryId, historyMessages);
            }
        }

        MessageWindowChatMemory chatMemory = MessageWindowChatMemory.builder()
                .id(memoryId)
                .maxMessages(MEMORY_MAX_MESSAGES)
                .chatMemoryStore(redisChatMemoryStore)
                .build();
        MessageWindowChatMemory vueChatMemory = MessageWindowChatMemory.builder()
                .id(memoryId + ":vue")
                .maxMessages(VUE_MEMORY_MAX_MESSAGES)
                .chatMemoryStore(redisChatMemoryStore)
                .build();
        AICodeGeneratorService plain = AiServices.builder(AICodeGeneratorService.class)
                .chatModel(qwenChatModel)
                .streamingChatModel(qwenStreamingChatModel)
                .chatMemory(chatMemory)
                .build();
        VueCodeGeneratorService vue = AiServices.builder(VueCodeGeneratorService.class)
                .streamingChatModel(qwenStreamingChatModel)
                .chatMemoryProvider(invocationMemoryId -> {
                    if (!appId.equals(invocationMemoryId)) {
                        throw new IllegalArgumentException("memoryId does not match appId");
                    }
                    return vueChatMemory;
                })
                .tools(new FileWriteTool())
                .maxToolCallingRoundTrips(VUE_MAX_TOOL_ROUND_TRIPS)
                .build();
        return new AppAiServices(plain, vue);
    }

    private ChatMessage toChatMessage(ChatHistory history) {
        if (history == null || history.getMessage() == null || history.getMessageType() == null) {
            return null;
        }
        if ("user".equals(history.getMessageType())) {
            return UserMessage.from(history.getMessage());
        }
        if ("ai".equals(history.getMessageType())) {
            return AiMessage.from(history.getMessage());
        }
        return null;
    }

    public void evictAppChatMemory(Long appId) {
        if (appId == null || appId <= 0) {
            return;
        }
        serviceCache.invalidate(appId);
        try {
            redisChatMemoryStore.deleteMessages(memoryId(appId));
        } catch (RuntimeException e) {
            log.warn("清理应用 Redis 对话记忆失败，appId={}", appId, e);
        }
    }

    private String memoryId(Long appId) {
        return "app:" + appId;
    }

    private record AppAiServices(AICodeGeneratorService plain, VueCodeGeneratorService vue) {
    }
}
