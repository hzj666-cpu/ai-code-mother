package com.hzj.aicodemother.ai.model;

import dev.langchain4j.model.output.structured.Description;
import lombok.Data;

@Description("多文件（HTML/CSS/JS分离）代码生成结果")
@Data
public class MultiFileCodeResult {

    @Description("index.html 的完整源码：以<!DOCTYPE html>开头，<head>中通过<link>引用style.css，</body>前通过<script>引用script.js；字段值为纯代码本身，禁止包含markdown代码块围栏、文件名标注或任何解释文字")
    private String htmlCode;

    @Description("style.css 的完整样式源码：包含网站全部样式规则，使用Flexbox或Grid实现响应式；字段值为纯代码本身，禁止包含markdown围栏或解释文字")
    private String cssCode;

    @Description("script.js 的完整交互逻辑源码：全部用原生JavaScript实现，禁止引用任何外部库；字段值为纯代码本身，禁止包含markdown围栏或解释文字")
    private String jsCode;

    @Description("对生成代码的简短中文说明：实现了哪些功能、有哪些亮点，200字以内；禁止把代码写进该字段")
    private String description;
}

