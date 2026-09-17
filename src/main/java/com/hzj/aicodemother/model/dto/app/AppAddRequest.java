package com.hzj.aicodemother.model.dto.app;

import lombok.Data;

import java.io.Serializable;

/**
 * 用户添加应用请求
 */
@Data
public class AppAddRequest implements Serializable {
    /**
     * 应用初始化的 prompt
     */
    private String initPrompt;
    private static final long serialVersionUID = 1L;


}
