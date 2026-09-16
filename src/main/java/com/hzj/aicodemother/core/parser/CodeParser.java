package com.hzj.aicodemother.core.parser;

import com.hzj.aicodemother.ai.model.enums.CodeGenTypeEnum;

/**
 * 代码解析策略接口（策略模式）
 *
 * 职责：把"AI 原始输出全文"（markdown 围栏风格）解析为强类型结果对象，
 * 供保存模板落盘。目前服务于流式链路攒出的全文；
 * 同步链路由 langchain4j 结构化返回直接产出结果对象，不走本策略。
 *
 * @param <T> 该模式对应的结果类型（如 HtmlCodeResult / MultiFileCodeResult）
 */
public interface CodeParser<T> {

    /**
     * 本策略支持的生成类型（执行器据此建立分发映射）
     */
    CodeGenTypeEnum supportedType();

    /**
     * 解析 AI 输出全文
     *
     * @param content 原始全文（含 markdown 围栏）
     * @return 强类型结果对象
     * @throws com.hzj.aicodemother.exception.BusinessException 输出不完整时抛出（含明细）
     */
    T parse(String content);
}
