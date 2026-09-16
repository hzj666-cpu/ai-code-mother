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
 * 任一语言块缺失时抛出带明细的业务异常（html:true, css:false, js:true），
 * 由 Controller 的 error 事件透出给前端。
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
        if (htmlCode == null || cssCode == null || jsCode == null) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR,
                    StrUtil.format("AI 输出不完整（html:{}, css:{}, js:{}），无法保存",
                            htmlCode != null, cssCode != null, jsCode != null));
        }
        MultiFileCodeResult result = new MultiFileCodeResult();
        result.setHtmlCode(htmlCode);
        result.setCssCode(cssCode);
        result.setJsCode(jsCode);
        return result;
    }
}
