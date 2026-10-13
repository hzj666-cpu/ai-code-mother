package com.hzj.aicodemother.model.dto.chathistory;

import com.hzj.aicodemother.common.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

@EqualsAndHashCode(callSuper = true)
@Data
public class ChatHistoryQueryRequest extends PageRequest implements Serializable {

    private Long id;

    private String message;

    private String messageType;

    private Long appId;

    private Long userId;

    /**
     * 光标：只返回比这个时间戳更早的记录。
     */
    private LocalDateTime beforeCreateTime;

    /**
     * Cursor tie breaker for records created in the same second.
     */
    private Long beforeId;

    @Serial
    private static final long serialVersionUID = 1L;
}
