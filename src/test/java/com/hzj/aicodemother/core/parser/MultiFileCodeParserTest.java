package com.hzj.aicodemother.core.parser;

import com.hzj.aicodemother.ai.model.MultiFileCodeResult;
import com.hzj.aicodemother.exception.BusinessException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * MultiFileCodeParser 单元测试：三块提取、js 别名、内联兜底拆分、缺 html 报错明细
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
    void parse_缺css和js块_从html内联标签兜底拆分() {
        // 模型违反格式约束：输出自包含 HTML，样式/脚本全部内联
        String content = """
                ```html
                <!DOCTYPE html>
                <html>
                <head>
                <style>body { margin: 0; }</style>
                </head>
                <body>
                <button id="btn">点我</button>
                <script>document.getElementById('btn').onclick = function() {};</script>
                </body>
                </html>
                ```
                """;
        MultiFileCodeResult result = multiFileCodeParser.parse(content);
        // 内联样式拆入 css
        assertTrue(result.getCssCode().contains("margin: 0"));
        // 内联脚本拆入 js
        assertTrue(result.getJsCode().contains("getElementById"));
        // html 中内联标签被移除，并注入了外链引用
        assertFalse(result.getHtmlCode().contains("<style>"));
        assertFalse(result.getHtmlCode().contains("onclick ="));
        assertTrue(result.getHtmlCode().contains("style.css"));
        assertTrue(result.getHtmlCode().contains("script.js"));
        // 页面结构保持完整
        assertTrue(result.getHtmlCode().contains("<button"));
        assertTrue(result.getHtmlCode().contains("</html>"));
    }

    @Test
    void parse_缺css和js块_且无内联内容_空文件兜底不再抛异常() {
        // 模型只输出了 html 块且无内联样式/脚本（如纯静态页），不再让整次生成作废
        String content = """
                ```html
                <html><body>hello</body></html>
                ```
                """;
        MultiFileCodeResult result = multiFileCodeParser.parse(content);
        assertTrue(result.getHtmlCode().contains("hello"));
        assertEquals("", result.getCssCode());
        assertEquals("", result.getJsCode());
    }

    @Test
    void parse_缺html块_仍抛异常且明细准确() {
        String content = """
                ```css
                body {}
                ```
                ```js
                console.log(1);
                ```
                """;
        BusinessException ex = assertThrows(BusinessException.class,
                () -> multiFileCodeParser.parse(content));
        assertTrue(ex.getMessage().contains("html:false"));
        assertTrue(ex.getMessage().contains("css:true"));
        assertTrue(ex.getMessage().contains("js:true"));
    }

    @Test
    void supportedType_是MULTI_FILE() {
        assertEquals(com.hzj.aicodemother.ai.model.enums.CodeGenTypeEnum.MULTI_FILE,
                multiFileCodeParser.supportedType());
    }
}
