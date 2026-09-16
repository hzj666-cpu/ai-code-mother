package com.hzj.aicodemother.controller;

import cn.hutool.core.util.StrUtil;
import com.hzj.aicodemother.ai.model.enums.CodeGenTypeEnum;
import com.hzj.aicodemother.core.AiCodeGeneratorFacade;
import com.hzj.aicodemother.exception.ErrorCode;
import com.hzj.aicodemother.exception.ThrowUtils;
import com.hzj.aicodemother.model.dto.codegen.CodeGenerateStreamRequest;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.concurrent.atomic.AtomicReference;

/**
 * AI 代码生成控制层（SSE 流式）
 *
 * Spring MVC 下 Flux<ServerSentEvent> 由 ReactiveTypeHandler 自动桥接为 SSE 响应，
 * 无需迁移 WebFlux。事件协议：
 * - message 事件（默认）：原始 token 分片，前端打字机追加渲染
 * - done 事件：流结束，data 为代码保存目录
 * - error 事件：流中异常，data 为错误信息
 */
@Slf4j
@RestController
@RequestMapping("/code")
public class AiCodeGeneratorController {

    @Resource
    private AiCodeGeneratorFacade aiCodeGeneratorFacade;

    /**
     * SSE 流式生成代码
     *
     * @param request 生成请求（提示词 + 生成类型）
     * @return SSE 事件流
     */
    @PostMapping(value = "/generate/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<String>> generateCodeStream(@RequestBody CodeGenerateStreamRequest request) {
        ThrowUtils.throwIf(request == null || StrUtil.isBlank(request.getUserMessage()),
                ErrorCode.PARAMS_ERROR, "提示词不能为空");
        CodeGenTypeEnum codeGenTypeEnum = CodeGenTypeEnum.getEnumByValue(request.getCodeGenType());
        ThrowUtils.throwIf(codeGenTypeEnum == null, ErrorCode.PARAMS_ERROR, "生成类型不合法");

        AtomicReference<String> savedDirRef = new AtomicReference<>();
        return aiCodeGeneratorFacade
                .generateAndSaveCodeStream(request.getUserMessage(), codeGenTypeEnum,
                        dir -> savedDirRef.set(dir.getAbsolutePath()))
                .map(chunk -> ServerSentEvent.<String>builder().data(chunk).build())
                .concatWith(Mono.fromSupplier(() ->
                        ServerSentEvent.<String>builder().event("done").data(savedDirRef.get()).build()))
                .onErrorResume(e -> {
                    log.error("流式代码生成失败", e);
                    return Flux.just(ServerSentEvent.<String>builder().event("error")
                            .data(StrUtil.blankToDefault(e.getMessage(), "生成失败，请稍后重试")).build());
                });
    }
}
