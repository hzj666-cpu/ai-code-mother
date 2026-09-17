package com.hzj.aicodemother.model.dto.app;

import lombok.Data;

import java.io.Serializable;

/**
 * 用户编辑自己应用请求（目前只支持修改应用名称）
 */
@Data
public class AppUpdateRequest implements Serializable {

    /**
     * id
     */
    private Long id;

    /**
     * 应用名称
     */
    private String appName;

    private static final long serialVersionUID = 1L;
}
