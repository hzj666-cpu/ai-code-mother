package com.hzj.aicodemother.ai.model;

import dev.langchain4j.model.output.structured.Description;
import lombok.Data;

@Description("HTML单页代码生成结果")
@Data
public class HtmlCodeResult {

    @Description("完整的HTML5文档源码：以<!DOCTYPE html>开头、以</html>结尾；CSS内联在<head>的<style>内，JS内联在</body>前的<script>内；字段值为纯代码本身，禁止包含markdown代码块围栏、文件名标注或任何解释文字")
    private String htmlCode;

    @Description("对生成代码的简短中文说明：实现了哪些功能、有哪些亮点，200字以内；禁止把代码写进该字段")
    private String description;
}

