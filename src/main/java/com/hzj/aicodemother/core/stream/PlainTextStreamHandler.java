package com.hzj.aicodemother.core.stream;

import com.hzj.aicodemother.ai.AICodeGeneratorService;
import com.hzj.aicodemother.ai.AICodeGeneratorServiceFactory;
import com.hzj.aicodemother.ai.model.enums.CodeGenTypeEnum;
import com.hzj.aicodemother.core.parser.CodeParserExecutor;
import com.hzj.aicodemother.core.saver.CodeFileSaverExecutor;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.io.File;
import java.util.Set;
import java.util.function.Consumer;

/** Preserves the existing raw token contract for HTML and multi-file modes. */
@Component
@RequiredArgsConstructor
public class PlainTextStreamHandler implements StreamHandler {

    private final AICodeGeneratorServiceFactory serviceFactory;
    private final CodeParserExecutor parserExecutor;
    private final CodeFileSaverExecutor saverExecutor;

    @Override
    public Set<CodeGenTypeEnum> supportedTypes() {
        return Set.of(CodeGenTypeEnum.HTML, CodeGenTypeEnum.MULTI_FILE);
    }

    @Override
    public Flux<String> handle(String userMessage, CodeGenTypeEnum type, Consumer<File> onSaved, Long appId) {
        return Flux.defer(() -> {
            AICodeGeneratorService service = serviceFactory.getAICodeGeneratorService(appId);
            StringBuilder buffer = new StringBuilder();
            Flux<String> stream = switch (type) {
                case HTML -> service.generateHtmlCodeStream(userMessage);
                case MULTI_FILE -> service.generateMultiFileCodeStream(userMessage);
                default -> throw new IllegalArgumentException("不支持原生文本流类型: " + type);
            };
            return stream.doOnNext(buffer::append).doOnComplete(() -> {
                Object result = parserExecutor.parse(type, buffer.toString());
                File dir = saverExecutor.save(type, result, appId);
                if (onSaved != null) {
                    onSaved.accept(dir);
                }
            });
        });
    }
}
