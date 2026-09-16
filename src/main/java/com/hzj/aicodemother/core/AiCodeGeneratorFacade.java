package com.hzj.aicodemother.core;

import com.hzj.aicodemother.ai.AICodeGeneratorService;
import com.hzj.aicodemother.ai.AICodeGeneratorServiceFactory;
import com.hzj.aicodemother.ai.model.enums.CodeGenTypeEnum;
import com.hzj.aicodemother.core.parser.CodeParserExecutor;
import com.hzj.aicodemother.core.saver.CodeFileSaverExecutor;
import com.hzj.aicodemother.exception.BusinessException;
import com.hzj.aicodemother.exception.ErrorCode;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.io.File;
import java.util.function.Consumer;

/**
 * AI 代码生成门面类，统一封装"生成 + 保存"完整流程
 *
 * 编排职责：
 * - 同步链路：langchain4j 结构化返回直接产出结果对象 -> 保存执行器落盘
 * - 流式链路：token 分片透传前端 -> 攒全文 -> 解析执行器 -> 保存执行器
 *
 * 解析与保存的类型差异分别下沉到 parser / saver 两个执行器，
 * 本类不感知具体结果类型；新增生成类型时本类除 service 接口方法外零改动。
 *
 * AiCodeGeneratorService 是 langchain4j AiServices 动态代理接口，不是 Spring Bean，
 * 不能直接 @Resource 注入，必须经 AICodeGeneratorServiceFactory 创建。
 */
@Service
public class AiCodeGeneratorFacade {

    @Resource
    private AICodeGeneratorServiceFactory aiCodeGeneratorServiceFactory;

    @Resource
    private CodeParserExecutor codeParserExecutor;

    @Resource
    private CodeFileSaverExecutor codeFileSaverExecutor;

    /**
     * 统一入口：根据类型生成并保存代码
     *
     * @param userMessage     用户提示词
     * @param codeGenTypeEnum 生成类型
     * @return 保存的目录
     */
    public File generateAndSaveCode(String userMessage, CodeGenTypeEnum codeGenTypeEnum) {
        if (codeGenTypeEnum == null) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "生成类型为空");
        }
        AICodeGeneratorService service = aiCodeGeneratorServiceFactory.createAICodeGeneratorService();
        Object result = switch (codeGenTypeEnum) {
            case HTML -> service.generateHtmlCodeResult(userMessage);
            case MULTI_FILE -> service.generateMultiFileCodeResult(userMessage);
        };
        return codeFileSaverExecutor.save(codeGenTypeEnum, result);
    }

    /**
     * 流式生成并保存代码（SSE 场景统一入口）
     *
     * 流式链路不支持结构化返回（AiServices 流式路径无 ServiceOutputParser），
     * 因此流式方法返回原始 token 分片 Flux<String>，本方法边推流边累积全文，
     * 流结束后经解析执行器剥离围栏、保存执行器落盘。
     *
     * @param userMessage     用户提示词
     * @param codeGenTypeEnum 生成类型
     * @param onSaved         落盘完成回调，参数为保存目录（供 Controller 追加 done 事件），可为 null
     * @return 原始 token 分片流（含 markdown 围栏，前端打字机直接渲染）
     */
    public Flux<String> generateAndSaveCodeStream(String userMessage, CodeGenTypeEnum codeGenTypeEnum,
                                                  Consumer<File> onSaved) {
        if (codeGenTypeEnum == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "生成类型为空");
        }
        AICodeGeneratorService service = aiCodeGeneratorServiceFactory.createAICodeGeneratorService();
        StringBuilder buffer = new StringBuilder();
        Flux<String> stream = switch (codeGenTypeEnum) {
            case HTML -> service.generateHtmlCodeStream(userMessage);
            case MULTI_FILE -> service.generateMultiFileCodeStream(userMessage);
        };
        return stream
                .doOnNext(buffer::append)
                .doOnComplete(() -> {
                    Object result = codeParserExecutor.parse(codeGenTypeEnum, buffer.toString());
                    File dir = codeFileSaverExecutor.save(codeGenTypeEnum, result);
                    if (onSaved != null) {
                        onSaved.accept(dir);
                    }
                });
    }
}
