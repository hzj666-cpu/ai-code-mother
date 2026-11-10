package com.hzj.aicodemother.ai.tool;

import cn.hutool.core.io.FileUtil;
import cn.hutool.json.JSONObject;
import com.hzj.aicodemother.constant.AppConstant;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.agent.tool.ToolMemoryId;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

/**
 * 文件写入工具
 * 支持 AI 通过工具调用的方式写入文件
 */
@Slf4j
@Component
public class FileWriteTool extends BaseTool{
    private static final long MAX_PROJECT_FILES = 128;

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
        Path path = VueProjectFilePolicy.resolve(appId, relativeFilePath, true);
        try {
            if (!Files.exists(path, LinkOption.NOFOLLOW_LINKS)) {
                Path projectRoot = Path.of(AppConstant.CODE_OUTPUT_ROOT_DIR, "vue_project_" + appId);
                long count = countFiles(projectRoot.resolve("src")) + countFiles(projectRoot.resolve("public"));
                if (count >= MAX_PROJECT_FILES) {
                    throw new IllegalArgumentException("工程文件数量已达上限");
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

    private long countFiles(Path directory) throws IOException {
        if (!Files.isDirectory(directory)) return 0;
        try (var files = Files.walk(directory)) {
            return files.filter(Files::isRegularFile).count();
        }
    }

    @Override
    public String getToolName() {
        return "writeFile";
    }

    @Override
    public String getDisplayName() {
        return "写入文件";
    }

    @Override
    public String generateToolExecutedResult(JSONObject arguments) {
        String relativeFilePath = arguments.getStr("relativeFilePath");
        String suffix = FileUtil.getSuffix(relativeFilePath);
        String content = arguments.getStr("content");
        return String.format("""
                        [工具调用] %s %s
                        ```%s
                        %s
                        ```
                        """, getDisplayName(), relativeFilePath, suffix, content);
    }
}
