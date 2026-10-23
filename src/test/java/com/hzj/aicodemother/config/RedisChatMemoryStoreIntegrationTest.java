package com.hzj.aicodemother.config;

import dev.langchain4j.community.store.memory.chat.redis.RedisChatMemoryStore;
import dev.langchain4j.data.message.UserMessage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** 手动验证普通 Redis 的聊天记忆读写；不依赖 Spring 或外部模型。 */
@EnabledIfEnvironmentVariable(named = "RUN_REDIS_MEMORY_TEST", matches = "true")
class RedisChatMemoryStoreIntegrationTest {

    @Test
    void readsAndWritesMessagesUsingRedisStrings() {
        RedisChatMemoryStoreConfig config = new RedisChatMemoryStoreConfig();
        config.setHost("localhost");
        config.setPort(6379);
        config.setPrefix("aicode:test:chat-memory:");
        RedisChatMemoryStore store = config.redisChatMemoryStore();
        String memoryId = UUID.randomUUID().toString();

        try {
            assertTrue(store.getMessages(memoryId).isEmpty());
            store.updateMessages(memoryId, List.of(UserMessage.from("hello")));
            assertEquals(1, store.getMessages(memoryId).size());
        } finally {
            store.deleteMessages(memoryId);
        }
    }
}
