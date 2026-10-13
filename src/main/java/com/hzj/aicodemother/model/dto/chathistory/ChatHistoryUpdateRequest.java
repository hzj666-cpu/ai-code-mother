package com.hzj.aicodemother.model.dto.chathistory;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * Request for updating a chat history record.
 */
@Data
public class ChatHistoryUpdateRequest implements Serializable {

    private Long id;

    private String message;

    private String messageType;

    @Serial
    private static final long serialVersionUID = 1L;
}
