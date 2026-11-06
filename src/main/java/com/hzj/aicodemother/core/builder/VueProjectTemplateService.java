package com.hzj.aicodemother.core.builder;

import cn.hutool.core.io.FileUtil;
import com.hzj.aicodemother.constant.AppConstant;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.LinkOption;
import java.util.List;

/** 固定、不可由 AI 修改的 Vue 工程基础文件。 */
@Component
public class VueProjectTemplateService {
    public static final String VERSION = "vue-base-v1";
    public static final String MARKER = ".platform-template";
    private static final String RESOURCE_ROOT = "vue-templates/" + VERSION + "/";
    private static final List<String> FILES = List.of("package.json", "package-lock.json", "index.html",
            "vite.config.js", "src/main.js", "src/App.vue", "src/router/index.js", "src/pages/HomePage.vue");
    private static final List<String> PROTECTED = List.of("package.json", "package-lock.json", "index.html",
            "vite.config.js", "src/main.js");

    public Path projectDir(Long appId) {
        if (appId == null || appId <= 0) {
            throw new IllegalArgumentException("应用 ID 不合法");
        }
        return Path.of(AppConstant.CODE_OUTPUT_ROOT_DIR, "vue_project_" + appId).toAbsolutePath().normalize();
    }

    public synchronized Path ensureInitialized(Long appId, String templateVersion) {
        if (!VERSION.equals(templateVersion)) {
            throw new IllegalStateException("旧版 Vue 工程须先迁移模板，现有文件不会自动覆盖");
        }
        Path project = projectDir(appId);
        if (Files.exists(project, LinkOption.NOFOLLOW_LINKS)) {
            verifyProject(project);
            return project;
        }
        Path staged = null;
        try {
            Files.createDirectories(project.getParent());
            staged = Files.createTempDirectory(project.getParent(), "vue-init-");
            for (String file : FILES) {
                Path target = staged.resolve(file);
                Files.createDirectories(target.getParent());
                try (InputStream input = new ClassPathResource(RESOURCE_ROOT + file).getInputStream()) {
                    Files.copy(input, target);
                }
            }
            Files.writeString(staged.resolve(MARKER), VERSION);
            Files.move(staged, project, StandardCopyOption.ATOMIC_MOVE);
            return project;
        } catch (IOException e) {
            throw new IllegalStateException("初始化 Vue 工程模板失败", e);
        } finally {
            if (staged != null && Files.exists(staged)) {
                FileUtil.del(staged.toFile());
            }
        }
    }

    public void verifyProject(Path project) {
        try {
            if (Files.isSymbolicLink(project) || Files.isSymbolicLink(project.resolve("src"))
                    || !Files.isRegularFile(project.resolve(MARKER), LinkOption.NOFOLLOW_LINKS)
                    || !VERSION.equals(Files.readString(project.resolve(MARKER)))) {
                throw new IllegalStateException("工程不是受管模板，禁止直接构建");
            }
            for (String file : PROTECTED) {
                Path target = project.resolve(file);
                if (Files.isSymbolicLink(target) || !Files.isRegularFile(target, LinkOption.NOFOLLOW_LINKS)) {
                    throw new IllegalStateException("模板保护文件缺失: " + file);
                }
                try (InputStream input = new ClassPathResource(RESOURCE_ROOT + file).getInputStream()) {
                    if (!java.util.Arrays.equals(input.readAllBytes(), Files.readAllBytes(target))) {
                        throw new IllegalStateException("模板保护文件被修改: " + file);
                    }
                }
            }
        } catch (IOException e) {
            throw new IllegalStateException("校验 Vue 工程模板失败", e);
        }
    }
}
