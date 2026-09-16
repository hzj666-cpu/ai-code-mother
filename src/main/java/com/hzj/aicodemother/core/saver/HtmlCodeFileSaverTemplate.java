package com.hzj.aicodemother.core.saver;

import com.hzj.aicodemother.ai.model.HtmlCodeResult;
import com.hzj.aicodemother.ai.model.enums.CodeGenTypeEnum;
import org.springframework.stereotype.Component;

/**
 * HTML 单文件模式保存模板：只写一个 index.html
 */
@Component
public class HtmlCodeFileSaverTemplate extends AbstractCodeFileSaverTemplate<HtmlCodeResult> {

    @Override
    protected CodeGenTypeEnum getBizType() {
        return CodeGenTypeEnum.HTML;
    }

    @Override
    protected Class<HtmlCodeResult> resultType() {
        return HtmlCodeResult.class;
    }

    @Override
    protected void writeFiles(String dirPath, HtmlCodeResult result) {
        writeToFile(dirPath, "index.html", result.getHtmlCode());
    }
}
