package com.hzj.aicodemother.ai;

import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.TokenStream;
import dev.langchain4j.service.UserMessage;

/** Vue 工程通过工具写文件；TokenStream 同时提供文本和工具执行事件。 */
public interface VueCodeGeneratorService {

    @SystemMessage(fromResource = "prompt/codegen-vue-system-prompt.txt")
    TokenStream generateVueProject(@MemoryId Long appId, @UserMessage String userMessage);
}
