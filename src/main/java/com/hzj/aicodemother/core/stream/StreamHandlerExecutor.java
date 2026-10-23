package com.hzj.aicodemother.core.stream;

import com.hzj.aicodemother.ai.model.enums.CodeGenTypeEnum;
import com.hzj.aicodemother.exception.BusinessException;
import com.hzj.aicodemother.exception.ErrorCode;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.io.File;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/** Dispatches each generation type to exactly one stream handler. */
@Component
public class StreamHandlerExecutor {

    private final Map<CodeGenTypeEnum, StreamHandler> handlers = new EnumMap<>(CodeGenTypeEnum.class);

    public StreamHandlerExecutor(List<StreamHandler> streamHandlers) {
        for (StreamHandler handler : streamHandlers) {
            for (CodeGenTypeEnum type : handler.supportedTypes()) {
                if (handlers.putIfAbsent(type, handler) != null) {
                    throw new IllegalStateException("重复的流处理器: " + type);
                }
            }
        }
        for (CodeGenTypeEnum type : CodeGenTypeEnum.values()) {
            if (!handlers.containsKey(type)) {
                throw new IllegalStateException("缺少流处理器: " + type);
            }
        }
    }

    public Flux<String> execute(String userMessage, CodeGenTypeEnum type, Consumer<File> onSaved, Long appId) {
        if (type == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "生成类型为空");
        }
        return handlers.get(type).handle(userMessage, type, onSaved, appId);
    }
}
