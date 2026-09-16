package com.hzj.aicodemother.observability;

import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.model.chat.listener.ChatModelErrorContext;
import dev.langchain4j.model.chat.listener.ChatModelListener;
import dev.langchain4j.model.chat.listener.ChatModelRequestContext;
import dev.langchain4j.model.chat.listener.ChatModelResponseContext;
import dev.langchain4j.model.output.TokenUsage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Qwen 模型调用日志与可观测性监听器
 *
 * 挂载机制：dashscope starter 的自动装配会收集容器中所有 ChatModelListener 类型的 Bean
 * （ObjectProvider<ChatModelListener>），注册到自动装配的 qwenChatModel 上，
 * 因此 @Component 即生效，无需改任何模型配置代码。
 *
 * 覆盖范围：所有经 qwenChatModel 的调用，包括 AiService 对话、RAG 注入后的请求、
 * 工具调用循环中的每一轮模型调用。
 */
@Component
@Slf4j
public class QwenLoggingListener implements ChatModelListener {

    /** 存放在 context attributes 中的本次调用开始时间戳（毫秒） */
    private static final String ATTR_START_TIME = "qwen.call.startTime";

    @Override
    public void onRequest(ChatModelRequestContext ctx) {
        ctx.attributes().put(ATTR_START_TIME, System.currentTimeMillis());
        List<ChatMessage> messages = ctx.chatRequest().messages();
        log.info("[Qwen观测] 请求发出：model={}, 消息数={}, 最后一条用户消息长度={}",
                ctx.chatRequest().modelName(),
                messages.size(),
                messages.isEmpty() ? 0 : textLength(messages.get(messages.size() - 1)));
    }

    @Override
    public void onResponse(ChatModelResponseContext ctx) {
        long duration = durationSince(ctx.attributes());
        TokenUsage usage = ctx.chatResponse().metadata().tokenUsage();
        if (usage != null) {
            log.info("[Qwen观测] 响应返回：耗时={}ms, 输入token={}, 输出token={}, 总token={}, finishReason={}",
                    duration, usage.inputTokenCount(), usage.outputTokenCount(),
                    usage.totalTokenCount(), ctx.chatResponse().metadata().finishReason());
        } else {
            log.info("[Qwen观测] 响应返回：耗时={}ms, token用量未上报, finishReason={}",
                    duration, ctx.chatResponse().metadata().finishReason());
        }
        logAiContent(ctx);
    }

    /**
     * 打印 AI 实际返回的正文内容。
     *
     * 正文在 ChatResponse.aiMessage() 中：
     * - 普通回复：aiMessage().text() 即模型输出内容
     * - 工具调用轮次：text() 可能为空/很短，真实载荷在 toolExecutionRequests()（模型决定调用哪些工具及参数），
     *   此时须看下一轮请求或工具执行日志才能看到最终答案
     */
    private void logAiContent(ChatModelResponseContext ctx) {
        dev.langchain4j.data.message.AiMessage aiMessage = ctx.chatResponse().aiMessage();
        if (aiMessage == null) {
            log.info("[Qwen观测] AI返回内容：<null>");
            return;
        }
        String text = aiMessage.text();
        if (aiMessage.hasToolExecutionRequests()) {
            log.info("[Qwen观测] AI返回内容（本轮为工具调用，共{}个）：{}",
                    aiMessage.toolExecutionRequests().size(),
                    aiMessage.toolExecutionRequests());
        }
        if (text != null && !text.isBlank()) {
            log.info("[Qwen观测] AI返回正文：\n{}", text);
        }
    }

    @Override
    public void onError(ChatModelErrorContext ctx) {
        log.error("[Qwen观测] 调用失败：耗时={}ms, model={}, 异常={}",
                durationSince(ctx.attributes()),
                ctx.chatRequest().modelName(),
                ctx.error().getMessage(),
                ctx.error());
    }

    private long durationSince(java.util.Map<Object, Object> attributes) {
        Object start = attributes.get(ATTR_START_TIME);
        return start instanceof Long s ? System.currentTimeMillis() - s : -1;
    }

    private int textLength(ChatMessage message) {
        try {
            return dev.langchain4j.data.message.UserMessage.class.isInstance(message)
                    ? ((dev.langchain4j.data.message.UserMessage) message).singleText().length()
                    : String.valueOf(message).length();
        } catch (Exception e) {
            return -1;
        }
    }
}
