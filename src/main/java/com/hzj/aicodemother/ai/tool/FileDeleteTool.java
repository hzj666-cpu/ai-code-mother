package com.hzj.aicodemother.ai.tool;

import cn.hutool.json.JSONObject;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.agent.tool.ToolMemoryId;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * 文件删除工具
 * 支持 AI 通过工具调用的方式删除文件
 */
@Slf4j
@Component
public class FileDeleteTool extends BaseTool{

    @Tool("删除指定路径的文件")
    public String deleteFile(
            @P("文件的相对路径")
            String relativeFilePath,
            @ToolMemoryId Long appId
    ) {
        try {
            Path path = VueProjectFilePolicy.resolve(appId, relativeFilePath, true);
            if (!Files.exists(path)) {
                return "警告：文件不存在，无需删除 - " + relativeFilePath;
            }
            if (!Files.isRegularFile(path)) {
                return "错误：指定路径不是文件，无法删除 - " + relativeFilePath;
            }
            Files.delete(path);
            log.info("成功删除文件: {}", path.toAbsolutePath());
            return "文件删除成功: " + relativeFilePath;
        } catch (IOException e) {
            String errorMessage = "删除文件失败: " + relativeFilePath + ", 错误: " + e.getMessage();
            log.error(errorMessage, e);
            return errorMessage;
        }
    }

    /**
     * 文件删除工具
     * 支持 AI 通过工具调用的方式删除文件
     */
        // 核心方法不变，此处省略

        @Override
        public String getToolName() {
            return "deleteFile";
        }

        @Override
        public String getDisplayName() {
            return "删除文件";
        }

        @Override
        public String generateToolExecutedResult(JSONObject arguments) {
            String relativeFilePath = arguments.getStr("relativeFilePath");
            return String.format("[工具调用] %s %s", getDisplayName(), relativeFilePath);
        }


}
