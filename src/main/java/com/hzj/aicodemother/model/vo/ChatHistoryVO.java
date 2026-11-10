package com.hzj.aicodemother.model.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;
import com.hzj.aicodemother.model.dto.app.AppAssetVO;

/**
 * Chat history view object.
 */
@Data
public class ChatHistoryVO implements Serializable {

    private Long id;

    private String message;

    private String messageType;

    private Long appId;

    private Long userId;

    private LocalDateTime createTime;
    private List<AppAssetVO> assets;

    @Serial
    private static final long serialVersionUID = 1L;
}
