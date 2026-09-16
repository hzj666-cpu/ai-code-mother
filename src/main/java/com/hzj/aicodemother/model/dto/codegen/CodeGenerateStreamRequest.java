package com.hzj.aicodemother.model.dto.codegen;

import lombok.Data;

import java.io.Serializable;

/**
 * 流式代码生成请求
 */
@Data
public class CodeGenerateStreamRequest implements Serializable {

    /**
     * 用户提示词（网站描述）
     */
    private String userMessage;

    /**
     * 生成类型：html / multi_file
     *
     * @see com.hzj.aicodemother.ai.model.enums.CodeGenTypeEnum
     */
    private String codeGenType;

    private static final long serialVersionUID = 1L;
}
