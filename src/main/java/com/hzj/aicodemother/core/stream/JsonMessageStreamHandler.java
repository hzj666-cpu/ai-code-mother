package com.hzj.aicodemother.core.stream;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hzj.aicodemother.ai.AICodeGeneratorServiceFactory;
import com.hzj.aicodemother.ai.model.enums.CodeGenTypeEnum;
import com.hzj.aicodemother.ai.model.message.AiResponseMessage;
import com.hzj.aicodemother.ai.model.message.StreamMessage;
import com.hzj.aicodemother.ai.model.message.ToolExecutedMessage;
import com.hzj.aicodemother.ai.model.message.ToolRequestMessage;
import com.hzj.aicodemother.constant.AppConstant;
import com.hzj.aicodemother.exception.BusinessException;
import com.hzj.aicodemother.exception.ErrorCode;
import dev.langchain4j.service.TokenStream;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;

/** Converts Vue TokenStream callbacks to typed JSON chunks on the existing SSE data channel.
 * 将 Vue TokenStream 回调转换为现有 SSE 数据通道上的类型化 JSON 块。*/


@Component
@RequiredArgsConstructor
public class JsonMessageStreamHandler implements StreamHandler {

    private final AICodeGeneratorServiceFactory serviceFactory;
    private final ObjectMapper objectMapper;

    @Override
    public Set<CodeGenTypeEnum> supportedTypes() {
        return Set.of(CodeGenTypeEnum.VUE_PROJECT);
    }

    @Override
    public Flux<String> handle(String userMessage, CodeGenTypeEnum type, Consumer<File> onSaved, Long appId) {
        if (type != CodeGenTypeEnum.VUE_PROJECT) {
            throw new IllegalArgumentException("不支持 JSON 消息流类型: " + type);
        }
        return Flux.defer(() -> {
            Path projectDir = Path.of(AppConstant.CODE_OUTPUT_ROOT_DIR, "vue_project_" + appId);
            TokenStream tokenStream = serviceFactory.getVueCodeGeneratorService(appId)
                    .generateVueProject(appId, userMessage);
            return toFlux(tokenStream, projectDir, onSaved);
        });
    }

    /** TokenStream starts only after subscription; callbacks preserve model event order. */
    Flux<String> toFlux(TokenStream tokenStream, Path projectDir, Consumer<File> onSaved) {
        return Flux.create(sink -> {
            Set<String> seenToolIds = new HashSet<>();
            try {
                tokenStream.onPartialResponse(text -> emit(sink, new AiResponseMessage(text)))
                        .beforeToolExecution(event -> {
                            String toolId = event.request().id();
                            // Some providers omit the ID; only deduplicate when an ID can identify the call.
                            boolean firstCall;
                            synchronized (seenToolIds) {
                                firstCall = toolId == null || seenToolIds.add(toolId);
                            }
                            if (firstCall) {
                                emit(sink, new ToolRequestMessage(event.request()));
                            }
                        })
                        .onToolExecuted(execution -> {
                            emit(sink, new ToolExecutedMessage(execution));
                            if (execution.hasFailed()) {
                                sink.error(new BusinessException(ErrorCode.SYSTEM_ERROR, "Vue 文件写入失败"));
                            }
                        })
                        .onCompleteResponse(response -> {
                            if (sink.isCancelled()) {
                                return;
                            }
                            if (!Files.isRegularFile(projectDir.resolve("index.html"))
                                    || !Files.isRegularFile(projectDir.resolve("package.json"))
                                    || !Files.isRegularFile(projectDir.resolve("src/App.vue"))) {
                                sink.error(new BusinessException(ErrorCode.SYSTEM_ERROR, "Vue 工程缺少必要文件"));
                                return;
                            }
                            try {
                                if (onSaved != null) {
                                    onSaved.accept(projectDir.toFile());
                                }
                                sink.complete();
                            } catch (RuntimeException e) {
                                sink.error(e);
                            }
                        })
                        .onError(sink::error)
                        .start();
            } catch (RuntimeException e) {
                sink.error(e);
            }
        });
    }

    private void emit(FluxSink<String> sink, StreamMessage message) {
        if (sink.isCancelled()) {
            return;
        }
        try {
            sink.next(objectMapper.writeValueAsString(message));
        } catch (JsonProcessingException e) {
            sink.error(e);
        }
    }
}
