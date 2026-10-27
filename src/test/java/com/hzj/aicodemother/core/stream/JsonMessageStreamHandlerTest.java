//package com.hzj.aicodemother.core.stream;
//
//import com.fasterxml.jackson.databind.JsonNode;
//import com.fasterxml.jackson.databind.ObjectMapper;
//import com.hzj.aicodemother.ai.AICodeGeneratorServiceFactory;
//import dev.langchain4j.agent.tool.ToolExecutionRequest;
//import dev.langchain4j.service.TokenStream;
//import dev.langchain4j.service.tool.BeforeToolExecution;
//import dev.langchain4j.service.tool.ToolExecution;
//import org.junit.jupiter.api.Test;
//import org.junit.jupiter.api.io.TempDir;
//
//import java.io.File;
//import java.nio.file.Files;
//import java.nio.file.Path;
//import java.util.List;
//import java.util.concurrent.atomic.AtomicReference;
//import java.util.function.Consumer;
//
//import static org.junit.jupiter.api.Assertions.*;
//import static org.mockito.ArgumentMatchers.any;
//import static org.mockito.Mockito.*;
//
//class JsonMessageStreamHandlerTest {
//
//    @TempDir
//    Path projectDir;
//
//    @Test
//    void convertsTextAndToolCallbacksAndCompletesAfterFilesExist() throws Exception {
//        TokenStream stream = mock(TokenStream.class);
//        AtomicReference<Consumer<String>> textCallback = new AtomicReference<>();
//        AtomicReference<Consumer<BeforeToolExecution>> requestCallback = new AtomicReference<>();
//        AtomicReference<Consumer<ToolExecution>> resultCallback = new AtomicReference<>();
//        AtomicReference<Consumer<dev.langchain4j.model.chat.response.ChatResponse>> completeCallback = new AtomicReference<>();
//        when(stream.onPartialResponse(any())).thenAnswer(invocation -> {
//            textCallback.set(invocation.getArgument(0));
//            return stream;
//        });
//        when(stream.beforeToolExecution(any())).thenAnswer(invocation -> {
//            requestCallback.set(invocation.getArgument(0));
//            return stream;
//        });
//        when(stream.onToolExecuted(any())).thenAnswer(invocation -> {
//            resultCallback.set(invocation.getArgument(0));
//            return stream;
//        });
//        when(stream.onCompleteResponse(any())).thenAnswer(invocation -> {
//            completeCallback.set(invocation.getArgument(0));
//            return stream;
//        });
//        when(stream.onError(any())).thenReturn(stream);
//
//        ToolExecutionRequest request = ToolExecutionRequest.builder()
//                .id("call-1").name("writeFile").arguments("{\"relativeFilePath\":\"index.html\"}").build();
//        BeforeToolExecution before = mock(BeforeToolExecution.class);
//        when(before.request()).thenReturn(request);
//        ToolExecution execution = mock(ToolExecution.class);
//        when(execution.request()).thenReturn(request);
//        when(execution.result()).thenReturn("文件写入成功: index.html");
//        doAnswer(invocation -> {
//            textCallback.get().accept("正在生成");
//            requestCallback.get().accept(before);
//            requestCallback.get().accept(before); // Repeated callback for the same tool ID must not duplicate the request.
//            resultCallback.get().accept(execution);
//            Files.createDirectories(projectDir.resolve("src"));
//            Files.writeString(projectDir.resolve("index.html"), "<html></html>");
//            Files.writeString(projectDir.resolve("package.json"), "{}");
//            Files.writeString(projectDir.resolve("src/App.vue"), "<template></template>");
//            completeCallback.get().accept(null);
//            return null;
//        }).when(stream).start();
//
//        AtomicReference<File> savedDir = new AtomicReference<>();
//        JsonMessageStreamHandler handler = new JsonMessageStreamHandler(mock(AICodeGeneratorServiceFactory.class), new ObjectMapper());
//        List<String> chunks = handler.toFlux(stream, projectDir, savedDir::set).collectList().block();
//
//        assertNotNull(chunks);
//        assertEquals(3, chunks.size());
//        ObjectMapper mapper = new ObjectMapper();
//        JsonNode text = mapper.readTree(chunks.get(0));
//        JsonNode toolRequest = mapper.readTree(chunks.get(1));
//        JsonNode toolResult = mapper.readTree(chunks.get(2));
//        assertEquals("ai_response", text.path("type").asText());
//        assertEquals("正在生成", text.path("data").asText());
//        assertEquals("tool_request", toolRequest.path("type").asText());
//        assertEquals("call-1", toolRequest.path("id").asText());
//        assertEquals("tool_executed", toolResult.path("type").asText());
//        assertFalse(toolResult.path("failed").asBoolean());
//        assertEquals(projectDir.toFile(), savedDir.get());
//    }
//
//    @Test
//    void failedToolStopsStreamWithoutCallingSavedCallback() {
//        TokenStream stream = mock(TokenStream.class, RETURNS_SELF);
//        AtomicReference<Consumer<ToolExecution>> resultCallback = new AtomicReference<>();
//        when(stream.onToolExecuted(any())).thenAnswer(invocation -> {
//            resultCallback.set(invocation.getArgument(0));
//            return stream;
//        });
//        ToolExecution execution = mock(ToolExecution.class);
//        ToolExecutionRequest request = ToolExecutionRequest.builder()
//                .id("call-2").name("writeFile").arguments("{}").build();
//        when(execution.request()).thenReturn(request);
//        when(execution.hasFailed()).thenReturn(true);
//        doAnswer(invocation -> {
//            resultCallback.get().accept(execution);
//            return null;
//        }).when(stream).start();
//
//        Consumer<File> onSaved = mock(Consumer.class);
//        JsonMessageStreamHandler handler = new JsonMessageStreamHandler(mock(AICodeGeneratorServiceFactory.class), new ObjectMapper());
//
//        assertThrows(RuntimeException.class, () -> handler.toFlux(stream, projectDir, onSaved).collectList().block());
//        verifyNoInteractions(onSaved);
//    }
//}
