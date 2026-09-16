package com.hzj.aicodemother.core.saver;

import com.hzj.aicodemother.ai.model.HtmlCodeResult;
import com.hzj.aicodemother.ai.model.MultiFileCodeResult;
import com.hzj.aicodemother.ai.model.enums.CodeGenTypeEnum;
import com.hzj.aicodemother.exception.BusinessException;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.file.Files;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 保存模板 + 保存执行器单元测试（写盘断言 + 类型校验 + 启动 fail fast）
 */
class CodeFileSaverTest {

    private final HtmlCodeFileSaverTemplate htmlTemplate = new HtmlCodeFileSaverTemplate();
    private final MultiFileCodeFileSaverTemplate multiFileTemplate = new MultiFileCodeFileSaverTemplate();

    @Test
    void save_html模板_只写一个indexHtml() {
        HtmlCodeResult result = new HtmlCodeResult();
        result.setHtmlCode("<!DOCTYPE html><html></html>");
        File dir = htmlTemplate.save(result);
        try {
            assertTrue(new File(dir, "index.html").exists());
        } finally {
            cleanupQuietly(dir);
        }
    }

    @Test
    void save_multiFile模板_写三个文件() {
        MultiFileCodeResult result = new MultiFileCodeResult();
        result.setHtmlCode("<html></html>");
        result.setCssCode("body{}");
        result.setJsCode("console.log(1)");
        File dir = multiFileTemplate.save(result);
        try {
            assertTrue(new File(dir, "index.html").exists());
            assertTrue(new File(dir, "style.css").exists());
            assertTrue(new File(dir, "script.js").exists());
        } finally {
            cleanupQuietly(dir);
        }
    }

    @Test
    void save_类型不匹配_抛业务异常而非ClassCastException() {
        MultiFileCodeResult wrongType = new MultiFileCodeResult();
        BusinessException ex = assertThrows(BusinessException.class,
                () -> htmlTemplate.save(wrongType));
        assertTrue(ex.getMessage().contains("类型不匹配"));
    }

    @Test
    void save_内容为空_抛业务异常() {
        BusinessException ex = assertThrows(BusinessException.class,
                () -> htmlTemplate.save(null));
        assertTrue(ex.getMessage().contains("保存内容为空"));
    }

    @Test
    void executor_按类型分发保存() {
        CodeFileSaverExecutor executor =
                new CodeFileSaverExecutor(java.util.List.of(htmlTemplate, multiFileTemplate));
        HtmlCodeResult result = new HtmlCodeResult();
        result.setHtmlCode("<html></html>");
        File dir = executor.save(CodeGenTypeEnum.HTML, result);
        try {
            assertTrue(dir.exists() && dir.isDirectory());
        } finally {
            cleanupQuietly(dir);
        }
    }

    @Test
    void executor_漏配模板_构造时failFast() {
        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> new CodeFileSaverExecutor(java.util.List.of(htmlTemplate)));
        assertTrue(ex.getMessage().contains("缺少保存模板"));
    }

    @Test
    void executor_分发后类型不匹配_抛业务异常() {
        CodeFileSaverExecutor executor =
                new CodeFileSaverExecutor(java.util.List.of(htmlTemplate, multiFileTemplate));
        assertThrows(BusinessException.class,
                () -> executor.save(CodeGenTypeEnum.HTML, new MultiFileCodeResult()));
    }

    private static void cleanupQuietly(File dir) {
        if (dir == null) {
            return;
        }
        File[] files = dir.listFiles();
        if (files != null) {
            for (File file : files) {
                try {
                    Files.deleteIfExists(file.toPath());
                } catch (Exception ignored) {
                }
            }
        }
        try {
            Files.deleteIfExists(dir.toPath());
        } catch (Exception ignored) {
        }
    }
}
