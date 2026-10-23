package com.hzj.aicodemother.ai.tool;

import com.hzj.aicodemother.constant.AppConstant;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.agent.tool.ToolMemoryId;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

/**
 * 文件写入工具
 * 支持 AI 通过工具调用的方式写入文件
 */
@Slf4j
public class FileWriteTool {

    @Tool("写入文件到指定路径")
    public String writeFile(
            @P("文件的相对路径")
            String relativeFilePath,
            @P("要写入文件的内容")
            String content,
            @ToolMemoryId Long appId
    ) {
        if (appId == null || appId <= 0 || relativeFilePath == null || relativeFilePath.isBlank()
                || content == null || content.getBytes(StandardCharsets.UTF_8).length > 1024 * 1024) {
            throw new IllegalArgumentException("文件写入参数不合法");
        }
        Path relativePath = Paths.get(relativeFilePath);
        if (relativePath.isAbsolute() || relativePath.toString().contains(":")) {
            throw new IllegalArgumentException("只能写入工程内的相对路径");
        }
        Path projectRoot = Paths.get(AppConstant.CODE_OUTPUT_ROOT_DIR, "vue_project_" + appId)
                .toAbsolutePath().normalize();
        Path path = projectRoot.resolve(relativePath).normalize();
        if (!path.startsWith(projectRoot) || path.equals(projectRoot)) {
            throw new IllegalArgumentException("文件路径超出工程目录");
        }
        try {
            Files.createDirectories(projectRoot);
            Path current = projectRoot;
            for (Path part : relativePath.normalize()) {
                current = current.resolve(part);
                if (Files.isSymbolicLink(current)) {
                    throw new IllegalArgumentException("不允许通过符号链接写文件");
                }
            }
            Files.createDirectories(path.getParent());
            if (Files.exists(path, LinkOption.NOFOLLOW_LINKS) && !Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS)) {
                throw new IllegalArgumentException("目标不是普通文件");
            }
            Files.write(path, content.getBytes(StandardCharsets.UTF_8),
                    StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING);
            log.info("Vue 文件写入成功, appId={}, path={}", appId, relativeFilePath);
            return "文件写入成功: " + relativeFilePath;
        } catch (IOException e) {
            log.error("Vue 文件写入失败, appId={}, path={}", appId, relativeFilePath, e);
            throw new UncheckedIOException("文件写入失败", e);
        }
    }
}
