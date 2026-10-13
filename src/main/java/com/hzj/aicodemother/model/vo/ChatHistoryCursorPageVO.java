package com.hzj.aicodemother.model.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Cursor pagination result for chat history.
 */
@Data
public class ChatHistoryCursorPageVO implements Serializable {

    private List<ChatHistoryVO> records;

    private long pageSize;

    private boolean hasMore;

    /**
     * Cursor timestamp for loading older records.
     */
    private LocalDateTime nextBeforeCreateTime;

    /**
     * Cursor id used to disambiguate records created in the same second.
     */
    private Long nextBeforeId;

    @Serial
    private static final long serialVersionUID = 1L;
}
