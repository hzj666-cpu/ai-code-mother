package com.hzj.aicodemother.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class AppServiceImplStreamHistoryTest {

    @Test
    void vueHistoryPreservesToolSummaryWithoutDuplicatingFileContent() {
        AppServiceImpl service = new AppServiceImpl();
        ReflectionTestUtils.setField(service, "objectMapper", new ObjectMapper());
        StringBuilder history = new StringBuilder();

        service.appendVueHistoryChunk("{\"type\":\"ai_response\",\"data\":\"正在生成\"}", history);
        service.appendVueHistoryChunk("{\"type\":\"tool_request\",\"name\":\"writeFile\",\"arguments\":\"secret-file-content\"}", history);
        service.appendVueHistoryChunk("{\"type\":\"tool_executed\",\"name\":\"writeFile\",\"result\":\"文件写入成功: src/App.vue\",\"failed\":false}", history);

        assertEquals("正在生成\n\n[选择工具] writeFile\n\n\n\n[工具执行] writeFile: 文件写入成功: src/App.vue\n\n", history.toString());
        assertFalse(history.toString().contains("secret-file-content"));
    }
}
