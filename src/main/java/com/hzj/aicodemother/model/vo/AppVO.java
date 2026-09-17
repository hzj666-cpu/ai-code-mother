package com.hzj.aicodemother.model.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 应用视图对象（脱敏后）用户查看用户详情
 */
@Data
public class AppVO implements Serializable {

    /**
     * id
     */
    private Long id;

    /**
     * 应用名称
     */
    private String appName;

    /**
     * 应用封面
     */
    private String cover;

    /**
     * 应用初始化的 prompt
     */
    private String initPrompt;

    /**
     * 代码生成类型：html/multi_file
     */
    private String codeGenType;

    /**
     * 部署标识
     */
    private String deployKey;

    /**
     * 部署时间
     */
    private LocalDateTime deployedTime;

    /**
     * 优先级
     */
    private Integer priority;

    /**
     * 创建用户 id
     */
    private Long userId;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 创建用户信息（脱敏）
     */
    private UserVO user;

    @Serial
    private static final long serialVersionUID = 1L;
}
