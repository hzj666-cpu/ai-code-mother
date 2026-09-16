package com.hzj.aicodemother.core.parser;

import com.hzj.aicodemother.ai.model.HtmlCodeResult;
import com.hzj.aicodemother.exception.BusinessException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * HtmlCodeParser 单元测试：围栏提取、\r\n 兼容、无语言标记降级、缺块报错
 */
class HtmlCodeParserTest {

    private final HtmlCodeParser htmlCodeParser = new HtmlCodeParser();

    @Test
    void parse_标准html围栏_提取纯代码() {
        String content = "好的，以下是代码：\n```html\n<!DOCTYPE html>\n<html></html>\n```\n以上是全部内容。";
        HtmlCodeResult result = htmlCodeParser.parse(content);
        assertTrue(result.getHtmlCode().trim().startsWith("<!DOCTYPE html>"));
        assertTrue(result.getHtmlCode().contains("</html>"));
    }

    @Test
    void parse_windows换行_rn_兼容() {
        String content = "```html\r\n<!DOCTYPE html>\r\n<html></html>\r\n```";
        HtmlCodeResult result = htmlCodeParser.parse(content);
        assertTrue(result.getHtmlCode().trim().startsWith("<!DOCTYPE html>"));
    }

    @Test
    void parse_无语言标记_doctype开头_降级认可() {
        String content = "```\n<!DOCTYPE html>\n<html></html>\n```";
        HtmlCodeResult result = htmlCodeParser.parse(content);
        assertTrue(result.getHtmlCode().trim().startsWith("<!DOCTYPE html>"));
    }

    @Test
    void parse_无代码块_抛业务异常() {
        String content = "抱歉，我无法完成该请求。";
        BusinessException ex = assertThrows(BusinessException.class,
                () -> htmlCodeParser.parse(content));
        assertTrue(ex.getMessage().contains("HTML 代码块"));
    }

    @Test
    void supportedType_是HTML() {
        assertEquals(com.hzj.aicodemother.ai.model.enums.CodeGenTypeEnum.HTML,
                htmlCodeParser.supportedType());
    }
}
