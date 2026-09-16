package com.hzj.aicodemother.core.parser;

import com.hzj.aicodemother.ai.model.MultiFileCodeResult;
import com.hzj.aicodemother.exception.BusinessException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * MultiFileCodeParser 单元测试：三块提取、js 别名、缺块报错明细
 */
class MultiFileCodeParserTest {

    private final MultiFileCodeParser multiFileCodeParser = new MultiFileCodeParser();

    @Test
    void parse_标准三块_全部提取() {
        String content = """
                ```html
                <!DOCTYPE html><html></html>
                ```
                ```css
                body { margin: 0; }
                ```
                ```js
                console.log(1);
                ```
                """;
        MultiFileCodeResult result = multiFileCodeParser.parse(content);
        assertTrue(result.getHtmlCode().contains("<html>"));
        assertTrue(result.getCssCode().contains("margin: 0"));
        assertTrue(result.getJsCode().contains("console.log"));
    }

    @Test
    void parse_javascript全称别名_同样识别() {
        String content = """
                ```html
                <html></html>
                ```
                ```css
                body {}
                ```
                ```javascript
                alert(1);
                ```
                """;
        MultiFileCodeResult result = multiFileCodeParser.parse(content);
        assertTrue(result.getJsCode().contains("alert"));
    }

    @Test
    void parse_缺css块_抛异常且明细准确() {
        String content = """
                ```html
                <html></html>
                ```
                ```js
                console.log(1);
                ```
                """;
        BusinessException ex = assertThrows(BusinessException.class,
                () -> multiFileCodeParser.parse(content));
        assertTrue(ex.getMessage().contains("html:true"));
        assertTrue(ex.getMessage().contains("css:false"));
        assertTrue(ex.getMessage().contains("js:true"));
    }

    @Test
    void supportedType_是MULTI_FILE() {
        assertEquals(com.hzj.aicodemother.ai.model.enums.CodeGenTypeEnum.MULTI_FILE,
                multiFileCodeParser.supportedType());
    }
}
