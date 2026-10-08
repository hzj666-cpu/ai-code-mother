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

    /** 匹配 HTML 中内联 <style> 标签（跨行） */
    private static final Pattern INLINE_STYLE = Pattern.compile("<style[^>]*>(.*?)</style>", Pattern.DOTALL | Pattern.CASE_INSENSITIVE);

    /** 匹配 HTML 中内联 <script> 标签（跨行），负向前瞻排除带 src 的外链脚本 */
    private static final Pattern INLINE_SCRIPT = Pattern.compile("<script(?![^>]*\\bsrc\\s*=)[^>]*>(.*?)</script>", Pattern.DOTALL | Pattern.CASE_INSENSITIVE);

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

    /**
     * 内联拆分结果：拆分后的 HTML + 拆出的 CSS / JS（无对应内容时为 null）
     */
    public record SplitResult(String html, String css, String js) {
    }

    /**
     * 兜底拆分：模型违反格式约束输出自包含 HTML（内联 style/script）时，
     * 将内联代码拆出为独立文件并注入外链引用，保证落盘后的三文件页面效果不变：
     * - 收集并移除所有内联 <style>，若 HTML 原本没有引用 style.css 则在 </head> 前注入 <link>
     * - 收集并移除所有非外链 <script>，若 HTML 原本没有引用 script.js 则在 </body> 前注入 <script src>
     *
     * @param html 自包含 HTML 全文
     * @return 拆分结果（css/js 无内联内容时为 null，不生成空文件引用）
     */
    public static SplitResult splitInlineCode(String html) {
        if (html == null || html.isBlank()) {
            return new SplitResult(html, null, null);
        }
        StringBuilder cssBuf = new StringBuilder();
        String stripped = removeMatches(INLINE_STYLE, html, cssBuf);
        StringBuilder jsBuf = new StringBuilder();
        stripped = removeMatches(INLINE_SCRIPT, stripped, jsBuf);
        String css = cssBuf.isEmpty() ? null : cssBuf.toString();
        String js = jsBuf.isEmpty() ? null : jsBuf.toString();
        if (css != null) {
            stripped = injectBefore(stripped, "</head>",
                    "<link rel=\"stylesheet\" href=\"style.css\">",
                    stripped.contains("style.css"));
        }
        if (js != null) {
            stripped = injectBefore(stripped, "</body>",
                    "<script src=\"script.js\"></script>",
                    stripped.contains("script.js"));
        }
        return new SplitResult(stripped, css, js);
    }

    /**
     * 移除 pattern 命中的所有片段，命中的分组内容（group(1)）收集到 collector（块间以空行分隔）
     */
    private static String removeMatches(Pattern pattern, String content, StringBuilder collector) {
        Matcher matcher = pattern.matcher(content);
        StringBuilder result = new StringBuilder();
        while (matcher.find()) {
            String code = matcher.group(1);
            if (code != null && !code.isBlank()) {
                if (!collector.isEmpty()) {
                    collector.append("\n\n");
                }
                collector.append(code.stripTrailing());
            }
            matcher.appendReplacement(result, Matcher.quoteReplacement(""));
        }
        matcher.appendTail(result);
        return result.toString();
    }

    /**
     * 在 anchor（如 </head>）前注入代码；anchor 不存在时注入到开头/结尾，已包含引用（alreadyReferenced）则跳过
     */
    private static String injectBefore(String html, String anchor, String code, boolean alreadyReferenced) {
        if (alreadyReferenced) {
            return html;
        }
        int idx = html.lastIndexOf(anchor);
        if (idx >= 0) {
            return html.substring(0, idx) + code + "\n" + html.substring(idx);
        }
        // 没有标准闭合标签（模型输出片段），css 注开头、js 注结尾兜底
        return "</head>".equals(anchor) ? code + "\n" + html : html + "\n" + code;
    }
}
