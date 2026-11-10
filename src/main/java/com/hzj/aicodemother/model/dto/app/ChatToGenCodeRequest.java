package com.hzj.aicodemother.model.dto.app;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * AI 对话生成代码请求
 */
@Data
public class ChatToGenCodeRequest implements Serializable {

    /**
     * 应用ID（决定生成类型与保存目录）
     */
    private Long appId;

    /**
     * 用户对话提示词
     */
    private String chatPrompt;

    /** Already uploaded image IDs; the server resolves ownership and purpose. */
    private List<Long> assetIds;

    private static final long serialVersionUID = 1L;
}
