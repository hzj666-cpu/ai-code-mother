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
import com.hzj.aicodemother.core.builder.VueProjectBuilder;
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
    private final VueProjectBuilder vueProjectBuilder;

    @Override
    public Set<CodeGenTypeEnum> supportedTypes() {
        return Set.of(CodeGenTypeEnum.VUE_PROJECT);
    }

/**
 * 处理Vue项目代码生成的请求
 *
 * @param userMessage 用户输入的消息
 * @param type 代码生成类型，必须是Vue项目类型
 * @param onSaved 代码生成完成后的回调函数，用于处理生成的文件
 * @param appId 应用ID，用于标识和区分不同的项目
 * @return 返回一个Flux<String>，用于流式返回生成的代码
 * @throws IllegalArgumentException 当传入的类型不是Vue项目类型时抛出异常
 */
    @Override
    public Flux<String> handle(String userMessage, CodeGenTypeEnum type, Consumer<File> onSaved, Long appId) {
    // 检查代码生成类型是否为Vue项目，如果不是则抛出异常
        if (type != CodeGenTypeEnum.VUE_PROJECT) {
            throw new IllegalArgumentException("不支持 JSON 消息流类型: " + type);
        }
    // 使用defer延迟执行，确保每次订阅都创建新的Flux
        return Flux.defer(() -> {
        // 创建Vue项目的输出目录路径，格式为：vue_project_ + appId
            Path projectDir = Path.of(AppConstant.CODE_OUTPUT_ROOT_DIR, "vue_project_" + appId);
        // 获取Vue代码生成服务并生成Vue项目代码
            TokenStream tokenStream = serviceFactory.getVueCodeGeneratorService(appId)
                    .generateVueProject(appId, userMessage);
        // 将TokenStream转换为Flux流，并处理生成的文件
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
                            // 文件已全部落盘，异步触发 npm install + build（内部虚拟线程，不阻塞 SSE 完成事件）
                            vueProjectBuilder.buildProjectAsync(projectDir.toString());
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
