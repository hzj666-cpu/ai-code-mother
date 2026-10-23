package com.hzj.aicodemother.ai.model.message;

import dev.langchain4j.service.tool.ToolExecution;
import lombok.Getter;

/** Result of a tool call; id correlates with the preceding request.
 * 工具调用结果；id 与之前的请求相关。 */
@Getter
public class ToolExecutedMessage extends StreamMessage {

    private final String id;
    private final String name;
    private final String result;
    private final boolean failed;

    public ToolExecutedMessage(ToolExecution execution) {
        super(StreamMessageTypeEnum.TOOL_EXECUTED.getValue());
        this.id = execution.request().id();
        this.name = execution.request().name();
        this.result = execution.result();
        this.failed = execution.hasFailed();
    }
}
