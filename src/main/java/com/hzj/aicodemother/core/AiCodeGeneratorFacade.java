package com.hzj.aicodemother.core;

import com.hzj.aicodemother.ai.AICodeGeneratorService;
import com.hzj.aicodemother.ai.AICodeGeneratorServiceFactory;
import com.hzj.aicodemother.ai.model.enums.CodeGenTypeEnum;
import com.hzj.aicodemother.core.saver.CodeFileSaverExecutor;
import com.hzj.aicodemother.core.stream.StreamHandlerExecutor;
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
 * - HTML/多文件流式链路：token 分片透传前端 -> 攒全文 -> 解析执行器 -> 保存执行器
 * - Vue 流式链路：工具直接写文件，JSON 消息处理器转发文本和工具事件
 *
 * 流式类型差异由 StreamHandlerExecutor 分发到各模式处理器。
 *
 * AiCodeGeneratorService 是 langchain4j AiServices 动态代理接口，不是 Spring Bean，
 * 不能直接 @Resource 注入，必须经 AICodeGeneratorServiceFactory 创建。
 */
@Service
public class AiCodeGeneratorFacade {

    @Resource
    private AICodeGeneratorServiceFactory aiCodeGeneratorServiceFactory;

    @Resource
    private CodeFileSaverExecutor codeFileSaverExecutor;

    @Resource
    private StreamHandlerExecutor streamHandlerExecutor;

    /** Initialize the app-specific AI Service and hydrate its Redis memory if needed. */
    public void initializeAppChatMemory(Long appId) {
        aiCodeGeneratorServiceFactory.getAICodeGeneratorService(appId);
    }

    /**
     * 统一入口：根据类型生成并保存代码
     *
     * @param userMessage     用户提示词
     * @param codeGenTypeEnum 生成类型
     * @param appId           应用ID（保存目录以 appId 命名）
     * @return 保存的目录
     */
    public File generateAndSaveCode(String userMessage, CodeGenTypeEnum codeGenTypeEnum, Long appId) {
        if (codeGenTypeEnum == null) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "生成类型为空");
        }
        //根据appId获取对应的AI服务实例
        AICodeGeneratorService service = aiCodeGeneratorServiceFactory.getAICodeGeneratorService(appId);
        Object result = switch (codeGenTypeEnum) {
            case HTML -> service.generateHtmlCodeResult(userMessage);
            case MULTI_FILE -> service.generateMultiFileCodeResult(userMessage);
            case VUE_PROJECT -> throw new BusinessException(ErrorCode.PARAMS_ERROR, "Vue 工程仅支持流式工具生成");
        };
        return codeFileSaverExecutor.save(codeGenTypeEnum, result, appId);
    }

    /**
     * 流式生成并保存代码（SSE 场景统一入口）
     *
     * 流式链路不支持结构化返回（AiServices 流式路径无 ServiceOutputParser），
     * 原生模式返回原始 token；Vue 模式返回带 type 的 JSON 消息。
     *
     * @param userMessage     用户提示词
     * @param codeGenTypeEnum 生成类型
     * @param onSaved         落盘完成回调，参数为保存目录（供 Controller 追加 done 事件），可为 null
     * @param appId           应用ID（保存目录以 appId 命名）
     * @return SSE data 内容流，具体格式由生成类型决定
     */
    public Flux<String> generateAndSaveCodeStream(String userMessage, CodeGenTypeEnum codeGenTypeEnum,
                                                  Consumer<File> onSaved, Long appId) {
        return streamHandlerExecutor.execute(userMessage, codeGenTypeEnum, onSaved, appId);
    }
}
