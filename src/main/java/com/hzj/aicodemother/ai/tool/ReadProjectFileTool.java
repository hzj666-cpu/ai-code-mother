package com.hzj.aicodemother.ai.tool;

import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.agent.tool.ToolMemoryId;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/** 向 AI 提供当前应用工程的受限源码读取能力。 */
public class ReadProjectFileTool {
    private static final long MAX_READ_BYTES = 64 * 1024;

    @Tool("读取当前 Vue 工程中的一个源码文件，修改已有文件前先读取")
    public String readFile(@P("工程内相对路径") String relativeFilePath, @ToolMemoryId Long appId) {
        Path path = VueProjectFilePolicy.resolve(appId, relativeFilePath, false);
        try {
            if (!Files.isRegularFile(path) || Files.size(path) > MAX_READ_BYTES) {
                throw new IllegalArgumentException("文件不存在或超过读取上限");
            }
            return Files.readString(path, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException("读取工程文件失败", e);
        }
    }
}
