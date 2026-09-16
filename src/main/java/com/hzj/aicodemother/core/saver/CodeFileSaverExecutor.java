package com.hzj.aicodemother.core.saver;

import com.hzj.aicodemother.ai.model.enums.CodeGenTypeEnum;
import com.hzj.aicodemother.exception.BusinessException;
import com.hzj.aicodemother.exception.ErrorCode;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * 保存执行器：按生成类型分发到对应的保存模板
 *
 * 新增生成类型时只需新增一个 AbstractCodeFileSaverTemplate 子类，
 * 本类与 Facade 均零改动；漏配模板在应用启动时即 fail fast。
 */
@Component
public class CodeFileSaverExecutor {

    private final Map<CodeGenTypeEnum, AbstractCodeFileSaverTemplate<?>> saverMap;

    public CodeFileSaverExecutor(List<AbstractCodeFileSaverTemplate<?>> templates) {
        saverMap = new EnumMap<>(CodeGenTypeEnum.class);
        for (AbstractCodeFileSaverTemplate<?> template : templates) {
            saverMap.put(template.getBizType(), template);
        }
        // 启动校验：每个生成类型都必须有对应模板
        for (CodeGenTypeEnum type : CodeGenTypeEnum.values()) {
            if (!saverMap.containsKey(type)) {
                throw new IllegalStateException("缺少保存模板: " + type);
            }
        }
    }

    /**
     * 按生成类型保存结果
     *
     * @param codeGenTypeEnum 生成类型
     * @param result          解析后的结果对象（类型由对应模板运行时校验）
     * @return 保存目录
     */
    public File save(CodeGenTypeEnum codeGenTypeEnum, Object result) {
        if (codeGenTypeEnum == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "生成类型为空");
        }
        AbstractCodeFileSaverTemplate<?> template = saverMap.get(codeGenTypeEnum);
        if (template == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "不支持的生成类型: " + codeGenTypeEnum);
        }
        return template.save(result);
    }
}
