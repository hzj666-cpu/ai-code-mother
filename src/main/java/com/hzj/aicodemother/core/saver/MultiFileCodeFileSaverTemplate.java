package com.hzj.aicodemother.core.saver;

import com.hzj.aicodemother.ai.model.MultiFileCodeResult;
import com.hzj.aicodemother.ai.model.enums.CodeGenTypeEnum;
import org.springframework.stereotype.Component;

/**
 * 多文件模式保存模板：写 index.html + style.css + script.js 三个文件
 */
@Component
public class MultiFileCodeFileSaverTemplate extends AbstractCodeFileSaverTemplate<MultiFileCodeResult> {

    @Override
    protected CodeGenTypeEnum getBizType() {
        return CodeGenTypeEnum.MULTI_FILE;
    }

    @Override
    protected Class<MultiFileCodeResult> resultType() {
        return MultiFileCodeResult.class;
    }

    @Override
    protected void writeFiles(String dirPath, MultiFileCodeResult result) {
        writeToFile(dirPath, "index.html", result.getHtmlCode());
        writeToFile(dirPath, "style.css", result.getCssCode());
        writeToFile(dirPath, "script.js", result.getJsCode());
    }
}
