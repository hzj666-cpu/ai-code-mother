package com.hzj.aicodemother.core;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Markdown 代码块解析器
 *
 * 流式生成走 markdown 代码块风格 prompt（前端打字机直接可读），
 * 流结束后由本类从全文中剥离围栏，取出各语言代码块的纯代码内容供落盘。
 */
public final class MarkdownCodeParser {

    /** 匹配 ```lang ... ``` 围栏代码块，语言标记可选，content 跨行 */
    private static final Pattern FENCED_BLOCK = Pattern.compile("```(\\w+)?\\s*\\r?\\n(.*?)```", Pattern.DOTALL);

    private MarkdownCodeParser() {
    }

    /**
     * 代码块：语言标记 + 纯代码内容
     */
    public record CodeBlock(String language, String content) {
    }

    /**
     * 提取全文中所有围栏代码块
     */
    public static List<CodeBlock> extractCodeBlocks(String markdown) {
        List<CodeBlock> blocks = new ArrayList<>();
        if (markdown == null || markdown.isBlank()) {
            return blocks;
        }
        Matcher matcher = FENCED_BLOCK.matcher(markdown);
        while (matcher.find()) {
            String language = matcher.group(1) == null ? "" : matcher.group(1).toLowerCase();
            String content = matcher.group(2);
            if (content != null && !content.isBlank()) {
                blocks.add(new CodeBlock(language, content.stripTrailing()));
            }
        }
        return blocks;
    }

    /**
     * 提取 HTML 单文件模式的代码：优先取 html 代码块；
     * 若模型没标语言，且唯一代码块内容以 <!DOCTYPE 或 <html 开头，也认可；
     * 都不满足时返回 null（由调用方决定报错或降级）。
     */
    public static String extractHtmlCode(String markdown) {
        List<CodeBlock> blocks = extractCodeBlocks(markdown);
        if (blocks.isEmpty()) {
            return null;
        }
        for (CodeBlock block : blocks) {
            if ("html".equals(block.language())) {
                return block.content();
            }
        }
        CodeBlock first = blocks.get(0);
        String head = first.content().trim().toLowerCase();
        if (first.language().isEmpty() && (head.startsWith("<!doctype") || head.startsWith("<html"))) {
            return first.content();
        }
        return blocks.size() == 1 ? first.content() : null;
    }

    /**
     * 按语言别名提取代码块（如 javascript/js），找不到返回 null
     */
    public static String extractCodeByLanguage(String markdown, String... languages) {
        for (CodeBlock block : extractCodeBlocks(markdown)) {
            for (String lang : languages) {
                if (lang.equalsIgnoreCase(block.language())) {
                    return block.content();
                }
            }
        }
        return null;
    }
}
