package com.hzj.aicodemother.core.parser;

import com.hzj.aicodemother.ai.model.HtmlCodeResult;
import com.hzj.aicodemother.ai.model.enums.CodeGenTypeEnum;
import com.hzj.aicodemother.exception.BusinessException;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 解析执行器单元测试：分发正确性 + 启动 fail fast + 空类型防御
 */
class CodeParserExecutorTest {

    private final CodeParserExecutor executor =
            new CodeParserExecutor(List.of(new HtmlCodeParser(), new MultiFileCodeParser()));

    @Test
    void parse_按类型分发到对应策略() {
        Object result = executor.parse(CodeGenTypeEnum.HTML, "```html\n<html></html>\n```");
        assertInstanceOf(HtmlCodeResult.class, result);
        assertTrue(((HtmlCodeResult) result).getHtmlCode().contains("<html>"));
    }

    @Test
    void parse_策略内异常原样透传() {
        assertThrows(BusinessException.class,
                () -> executor.parse(CodeGenTypeEnum.HTML, "没有代码块的内容"));
    }

    @Test
    void parse_类型为空_抛业务异常() {
        assertThrows(BusinessException.class,
                () -> executor.parse(null, "任意内容"));
    }

    @Test
    void constructor_漏配策略_启动failFast() {
        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> new CodeParserExecutor(List.of(new HtmlCodeParser())));
        assertTrue(ex.getMessage().contains("缺少解析策略"));
    }
}
