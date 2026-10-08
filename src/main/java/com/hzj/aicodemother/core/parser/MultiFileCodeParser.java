package com.hzj.aicodemother.core.parser;

import cn.hutool.core.util.StrUtil;
import com.hzj.aicodemother.ai.model.MultiFileCodeResult;
import com.hzj.aicodemother.ai.model.enums.CodeGenTypeEnum;
import com.hzj.aicodemother.core.MarkdownCodeParser;
import com.hzj.aicodemother.exception.BusinessException;
import com.hzj.aicodemother.exception.ErrorCode;
import org.springframework.stereotype.Component;

/**
 * 多文件模式解析策略：从全文按语言标记提取 html / css / js 三个代码块
 *
 * 容错策略（提示词约束不能 100% 保证模型输出格式，解析层必须兜底）：
 * - 三块齐全：直接提取
 * - 缺 css/js 块但 html 存在：将 html 中内联的 style/script 拆出为独立文件并注入外链引用
 * - 拆分后仍缺 css/js：以空文件落盘（页面自包含时不受影响，如纯静态页没有交互脚本是正常的）
 * - 连 html 块都没有：无法保存，抛出带明细的业务异常，由 Controller 的 error 事件透出给前端
 */
@Component
public class MultiFileCodeParser implements CodeParser<MultiFileCodeResult> {

    @Override
    public CodeGenTypeEnum supportedType() {
        return CodeGenTypeEnum.MULTI_FILE;
    }

    @Override
    public MultiFileCodeResult parse(String content) {
        String htmlCode = MarkdownCodeParser.extractCodeByLanguage(content, "html");
        String cssCode = MarkdownCodeParser.extractCodeByLanguage(content, "css");
        String jsCode = MarkdownCodeParser.extractCodeByLanguage(content, "javascript", "js");
        // 连 html 都没有，无法兜底
        if (htmlCode == null) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR,
                    StrUtil.format("AI 输出不完整（html:{}, css:{}, js:{}），无法保存",
                            false, cssCode != null, jsCode != null));
        }
        // 模型偶尔会把样式/脚本内联进 HTML（违反提示词约束），兜底拆分为三文件，避免整次生成作废
        if (cssCode == null || jsCode == null) {
            MarkdownCodeParser.SplitResult split = MarkdownCodeParser.splitInlineCode(htmlCode);
            htmlCode = split.html();
            if (cssCode == null) {
                cssCode = split.css();
            }
            if (jsCode == null) {
                jsCode = split.js();
            }
        }
        MultiFileCodeResult result = new MultiFileCodeResult();
        result.setHtmlCode(htmlCode);
        // 仍为 null 说明页面确实没有对应内容（自包含或无交互），落盘空文件，不再让整次生成失败
        result.setCssCode(cssCode == null ? "" : cssCode);
        result.setJsCode(jsCode == null ? "" : jsCode);
        return result;
    }
}
