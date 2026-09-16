package com.hzj.aicodemother.ai;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.service.AiServices;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

@Component
public class AICodeGeneratorServiceFactory {

    @Resource
    private ChatModel qwenChatModel;
    @Resource
    private StreamingChatModel qwenStreamingChatModel;

    public AICodeGeneratorService createAICodeGeneratorService() {
        return  AiServices.builder(AICodeGeneratorService.class)
                .chatModel(qwenChatModel)
                .streamingChatModel(qwenStreamingChatModel)
                .build();
    }
}
