package com.hzj.aicodemother.ai;

import com.hzj.aicodemother.model.entity.ChatHistory;
import com.hzj.aicodemother.service.ChatHistoryService;
import dev.langchain4j.community.store.memory.chat.redis.RedisChatMemoryStore;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AICodeGeneratorServiceFactoryTest {

    @Mock
    private ChatModel qwenChatModel;

    @Mock
    private StreamingChatModel qwenStreamingChatModel;

    @Mock
    private RedisChatMemoryStore redisChatMemoryStore;

    @Mock
    private ChatHistoryService chatHistoryService;

    @InjectMocks
    private AICodeGeneratorServiceFactory factory;


//    @Resource
//    private AICodeGeneratorServiceFactory aICodeGeneratorServiceFactory;
//
//    @Test
//    void generatorHtmlCode() {
//        AICodeGeneratorService aiCodeGeneratorService = aICodeGeneratorServiceFactory.createAICodeGeneratorService();
//        String s = aiCodeGeneratorService.generatorHtmlCode("做一个程序员拉丽的计算器小工具");
//        assertNotNull(s);
//    }
//    @Test
//    void generatorMultiCode() {
//        AICodeGeneratorService aiCodeGeneratorService = aICodeGeneratorServiceFactory.createAICodeGeneratorService();
//        String s = aiCodeGeneratorService.generatorMultiFileCode("做一个程序员拉丽的日历表");
//        assertNotNull(s);
//    }
//
//    @Test
//    void generateHtmlCodeResult(){
//        AICodeGeneratorService aiCodeGeneratorService = aICodeGeneratorServiceFactory.createAICodeGeneratorService();
//        HtmlCodeResult htmlCodeResult = aiCodeGeneratorService.generateHtmlCodeResult("做一个程序员拉丽的简单计算器小工具");
//        assertNotNull(htmlCodeResult);
//
//    }

    @Test
    void shouldCacheOneAiServicePerApp() {
        AICodeGeneratorService firstAppService = factory.getAICodeGeneratorService(101L);
        AICodeGeneratorService secondAppService = factory.getAICodeGeneratorService(202L);

        assertSame(firstAppService, factory.getAICodeGeneratorService(101L));
        assertNotSame(firstAppService, secondAppService);
    }

    @Test
    void shouldDeleteRedisMemoryAndEvictServiceWhenAppIsRemoved() {
        AICodeGeneratorService originalService = factory.getAICodeGeneratorService(101L);

        factory.evictAppChatMemory(101L);

        verify(redisChatMemoryStore).deleteMessages("app:101");
        assertNotSame(originalService, factory.getAICodeGeneratorService(101L));
    }

    @Test
    void shouldRestoreRecentDatabaseHistoryWhenRedisMemoryIsEmpty() {
        ChatHistory userHistory = ChatHistory.builder()
                .messageType("user")
                .message("make a blog")
                .build();
        ChatHistory aiHistory = ChatHistory.builder()
                .messageType("ai")
                .message("created a blog")
                .build();
        when(chatHistoryService.listLatestForMemory(303L, 10))
                .thenReturn(java.util.List.of(userHistory, aiHistory));

        factory.getAICodeGeneratorService(303L);

        verify(redisChatMemoryStore).updateMessages(eq("app:303"), argThat(messages ->
                messages.size() == 2
                        && messages.get(0) instanceof UserMessage
                        && messages.get(1) instanceof AiMessage));
    }

    @Test
    void shouldNotOverwriteExistingRedisMemoryWithDatabaseHistory() {
        when(redisChatMemoryStore.getMessages("app:404"))
                .thenReturn(java.util.List.<ChatMessage>of(UserMessage.from("existing memory")));

        factory.getAICodeGeneratorService(404L);

        verify(chatHistoryService, never()).listLatestForMemory(404L, 10);
        verify(redisChatMemoryStore, never()).updateMessages(eq("app:404"), org.mockito.ArgumentMatchers.anyList());
    }

    @Test
    void shouldRejectInvalidAppId() {
        assertThrows(IllegalArgumentException.class, () -> factory.getAICodeGeneratorService(null));
        assertThrows(IllegalArgumentException.class, () -> factory.getAICodeGeneratorService(0L));
    }
}
