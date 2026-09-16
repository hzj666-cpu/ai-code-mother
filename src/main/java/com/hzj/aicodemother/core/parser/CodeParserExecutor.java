package com.hzj.aicodemother.core.parser;

import com.hzj.aicodemother.ai.model.enums.CodeGenTypeEnum;
import com.hzj.aicodemother.exception.BusinessException;
import com.hzj.aicodemother.exception.ErrorCode;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * 解析执行器：按生成类型分发到对应的解析策略
 *
 * 新增生成类型时只需新增一个 CodeParser 实现，本类与 Facade 均零改动；
 * 漏配策略在应用启动时即 fail fast。
 *
 * 返回 Object 而非泛型 T：结果对象的类型由"同枚举值的保存模板"在落盘时
 * 运行时校验，调用方（Facade）无需感知具体类型，避免传播强转。
 */
@Component
public class CodeParserExecutor {

    private final Map<CodeGenTypeEnum, CodeParser<?>> parserMap;

    public CodeParserExecutor(List<CodeParser<?>> parsers) {
        parserMap = new EnumMap<>(CodeGenTypeEnum.class);
        for (CodeParser<?> parser : parsers) {
            parserMap.put(parser.supportedType(), parser);
        }
        // 启动校验：每个生成类型都必须有对应策略
        for (CodeGenTypeEnum type : CodeGenTypeEnum.values()) {
            if (!parserMap.containsKey(type)) {
                throw new IllegalStateException("缺少解析策略: " + type);
            }
        }
    }

    /**
     * 按生成类型解析 AI 输出全文
     *
     * @param codeGenTypeEnum 生成类型
     * @param content         AI 原始输出全文
     * @return 强类型结果对象（具体类型由对应策略决定）
     */
    public Object parse(CodeGenTypeEnum codeGenTypeEnum, String content) {
        if (codeGenTypeEnum == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "生成类型为空");
        }
        CodeParser<?> parser = parserMap.get(codeGenTypeEnum);
        if (parser == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "不支持的生成类型: " + codeGenTypeEnum);
        }
        return parser.parse(content);
    }
}
