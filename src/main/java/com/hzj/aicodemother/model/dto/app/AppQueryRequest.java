package com.hzj.aicodemother.model.dto.app;

import com.hzj.aicodemother.common.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;
import java.io.Serializable;

@EqualsAndHashCode(callSuper = true)
@Data
public class AppQueryRequest extends PageRequest implements Serializable {

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
     * 优先级（精选：大于 0）
     */
    private Integer priority;

    /**
     * 创建用户 id
     */
    private Long userId;

    @Serial
    private static final long serialVersionUID = 1L;
}
