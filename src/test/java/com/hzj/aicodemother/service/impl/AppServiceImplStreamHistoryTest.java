package com.hzj.aicodemother.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hzj.aicodemother.model.entity.ChatHistory;
import com.hzj.aicodemother.service.ChatHistoryService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AppServiceImplStreamHistoryTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    private AppServiceImpl newService(ChatHistoryService chatHistoryService) {
        AppServiceImpl service = new AppServiceImpl();
        ReflectionTestUtils.setField(service, "objectMapper", objectMapper);
        ReflectionTestUtils.setField(service, "chatHistoryService", chatHistoryService);
        return service;
    }

    @Test
    void vueHistoryAccumulatesAiTextOnly() {
        AppServiceImpl service = newService(mock(ChatHistoryService.class));
        StringBuilder history = new StringBuilder();
        Map<String, JsonNode> pending = new HashMap<>();

        service.handleVueStreamChunk("{\"type\":\"ai_response\",\"data\":\"正在生成\"}", history, pending, 1L, 1L);
        service.handleVueStreamChunk(
                "{\"type\":\"tool_request\",\"id\":\"t1\",\"name\":\"writeFile\",\"arguments\":\"{\\\"relativeFilePath\\\":\\\"src/App.vue\\\",\\\"content\\\":\\\"secret-file-content\\\"}\"}",
                history, pending, 1L, 1L);
        service.handleVueStreamChunk(
                "{\"type\":\"tool_executed\",\"id\":\"t1\",\"name\":\"writeFile\",\"result\":\"文件写入成功: src/App.vue\",\"failed\":false}",
                history, pending, 1L, 1L);

        // AI 缓冲只累积正文：工具摘要不再拼进 AI 历史消息，改由 tool 记录承载
        assertEquals("正在生成", history.toString());
        assertFalse(history.toString().contains("secret-file-content"));
    }

    @Test
    void toolExecutedSavesStructuredToolRecord() throws Exception {
        ChatHistoryService chatHistoryService = mock(ChatHistoryService.class);
        when(chatHistoryService.save(any(ChatHistory.class))).thenReturn(true);
        AppServiceImpl service = newService(chatHistoryService);
        StringBuilder history = new StringBuilder();
        Map<String, JsonNode> pending = new HashMap<>();

        service.handleVueStreamChunk(
                "{\"type\":\"tool_request\",\"id\":\"t1\",\"name\":\"writeFile\",\"arguments\":\"{\\\"relativeFilePath\\\":\\\"src/App.vue\\\",\\\"content\\\":\\\"<template>hi</template>\\\"}\"}",
                history, pending, 1L, 2L);
        service.handleVueStreamChunk(
                "{\"type\":\"tool_executed\",\"id\":\"t1\",\"name\":\"writeFile\",\"result\":\"文件写入成功: src/App.vue\",\"failed\":false}",
                history, pending, 1L, 2L);

        ArgumentCaptor<ChatHistory> captor = ArgumentCaptor.forClass(ChatHistory.class);
        verify(chatHistoryService).save(captor.capture());
        ChatHistory saved = captor.getValue();
        assertEquals("tool", saved.getMessageType());
        assertEquals(1L, saved.getAppId());
        assertEquals(2L, saved.getUserId());
        JsonNode payload = objectMapper.readTree(saved.getMessage());
        assertEquals("writeFile", payload.path("name").asText());
        assertEquals("src/App.vue", payload.path("filePath").asText());
        assertEquals("<template>hi</template>", payload.path("content").asText());
        assertEquals("文件写入成功: src/App.vue", payload.path("result").asText());
        assertFalse(payload.path("failed").asBoolean());
    }

    @Test
    void oversizedContentIsTruncatedAndPersistFailureDoesNotThrow() throws Exception {
        ChatHistoryService chatHistoryService = mock(ChatHistoryService.class);
        when(chatHistoryService.save(any(ChatHistory.class))).thenReturn(false);
        AppServiceImpl service = newService(chatHistoryService);
        StringBuilder history = new StringBuilder();
        Map<String, JsonNode> pending = new HashMap<>();

        String bigContent = "a".repeat(25_000);
        String arguments = objectMapper.createObjectNode()
                .put("relativeFilePath", "src/big.vue")
                .put("content", bigContent)
                .toString();
        String escaped = objectMapper.writeValueAsString(arguments);
        service.handleVueStreamChunk(
                "{\"type\":\"tool_request\",\"id\":\"t1\",\"name\":\"writeFile\",\"arguments\":" + escaped + "}",
                history, pending, 1L, 1L);
        // save 返回 false（落库失败）也不应抛异常中断生成
        service.handleVueStreamChunk(
                "{\"type\":\"tool_executed\",\"id\":\"t1\",\"name\":\"writeFile\",\"result\":\"r\",\"failed\":false}",
                history, pending, 1L, 1L);

        ArgumentCaptor<ChatHistory> captor = ArgumentCaptor.forClass(ChatHistory.class);
        verify(chatHistoryService).save(captor.capture());
        JsonNode payload = objectMapper.readTree(captor.getValue().getMessage());
        assertTrue(payload.path("content").asText().startsWith("a"));
        assertTrue(payload.path("content").asText().contains("已截断"));
    }
}
