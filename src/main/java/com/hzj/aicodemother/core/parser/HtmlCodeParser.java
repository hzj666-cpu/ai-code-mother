package com.hzj.aicodemother.core.parser;

import com.hzj.aicodemother.ai.model.HtmlCodeResult;
import com.hzj.aicodemother.ai.model.enums.CodeGenTypeEnum;
import com.hzj.aicodemother.core.MarkdownCodeParser;
import com.hzj.aicodemother.exception.BusinessException;
import com.hzj.aicodemother.exception.ErrorCode;
import org.springframework.stereotype.Component;

/**
 * HTML 单文件模式解析策略
 *
 * 委托 MarkdownCodeParser 提取代码块（含 \r\n 兼容、无语言标记降级链），
 * 提取不到时显式报错，不做"整段兜底"（避免解释文字污染落盘文件）。
 */
@Component
public class HtmlCodeParser implements CodeParser<HtmlCodeResult> {

    @Override
    public CodeGenTypeEnum supportedType() {
        return CodeGenTypeEnum.HTML;
    }

    @Override
    public HtmlCodeResult parse(String content) {
        String htmlCode = MarkdownCodeParser.extractHtmlCode(content);
        if (htmlCode == null) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "AI 未生成 HTML 代码块，无法保存");
        }
        HtmlCodeResult result = new HtmlCodeResult();
        result.setHtmlCode(htmlCode);
        return result;
    }
}
