package com.hzj.aicodemother.model.dto.chathistory;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * Request for creating a chat history record.
 */
@Data
public class ChatHistoryAddRequest implements Serializable {

    private Long appId;

    private String message;

    private String messageType;

    @Serial
    private static final long serialVersionUID = 1L;
}
