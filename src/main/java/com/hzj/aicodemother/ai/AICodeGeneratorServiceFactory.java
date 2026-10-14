package com.hzj.aicodemother.ai;

import com.hzj.aicodemother.model.entity.ChatHistory;
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

    private final Cache<Long, AICodeGeneratorService> serviceCache = Caffeine.newBuilder()
            .maximumSize(SERVICE_CACHE_MAXIMUM_SIZE)
            .expireAfterAccess(SERVICE_CACHE_IDLE_TTL)
            .build();

    public AICodeGeneratorService getAICodeGeneratorService(Long appId) {
        if (appId == null || appId <= 0) {
            throw new IllegalArgumentException("appId must be positive");
        }
        //生成appid对应的AICodeGeneratorService实例
        return serviceCache.get(appId, this::createAICodeGeneratorService);
    }

    private AICodeGeneratorService createAICodeGeneratorService(Long appId) {
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
        return AiServices.builder(AICodeGeneratorService.class)
                .chatModel(qwenChatModel)
                .streamingChatModel(qwenStreamingChatModel)
                .chatMemory(chatMemory)
                .build();
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
}
