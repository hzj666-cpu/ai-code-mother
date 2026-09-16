package com.hzj.aicodemother.ai;

import com.hzj.aicodemother.ai.model.HtmlCodeResult;
import com.hzj.aicodemother.ai.model.MultiFileCodeResult;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import reactor.core.publisher.Flux;

public interface AICodeGeneratorService {

/**
 * 生成HTMl
 * @param userMessage 用户输入的消息内容，将作为用户消息传递给系统
 * @return 返回生成的聊天响应内容
 */
    @SystemMessage(fromResource = "prompt/codegen-html-system-prompt.txt")
    String generatorHtmlCode(@UserMessage String userMessage);

    @SystemMessage(fromResource = "prompt/codegen-multi-file-system-prompt.txt")
    String generatorMultiFileCode(@UserMessage String userMessage);

    /**
     * 生成多文件代码
     * 返回结构化对象
     * @param userMessage 用户输入的消息内容，将作为用户消息传递给系统
     * @return 返回生成的聊天响应内容
     */
    @SystemMessage(fromResource = "prompt/codegen-multi-file-json-system-prompt.txt")
    MultiFileCodeResult generateMultiFileCodeResult(@UserMessage String userMessage);

    @SystemMessage(fromResource = "prompt/codegen-html-json-system-prompt.txt")
    HtmlCodeResult generateHtmlCodeResult(@UserMessage String userMessage);

    /**
     * 流式生成 HTML 单文件代码（SSE 打字机效果）
     * 注意：流式不支持结构化返回，必须返回 Flux<String>（langchain4j-reactor 适配），
     * prompt 用 markdown 代码块风格（前端直接可读），后端攒全文后自行解析围栏落盘
     */
    @SystemMessage(fromResource = "prompt/codegen-html-system-prompt.txt")
    Flux<String> generateHtmlCodeStream(@UserMessage String userMessage);

    /**
     * 流式生成多文件（HTML/CSS/JS）代码
     */
    @SystemMessage(fromResource = "prompt/codegen-multi-file-system-prompt.txt")
    Flux<String> generateMultiFileCodeStream(@UserMessage String userMessage);
}
