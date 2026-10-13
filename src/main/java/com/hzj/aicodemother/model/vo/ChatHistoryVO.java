package com.hzj.aicodemother.model.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

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

    @Serial
    private static final long serialVersionUID = 1L;
}
