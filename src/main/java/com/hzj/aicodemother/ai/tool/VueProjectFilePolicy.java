package com.hzj.aicodemother.ai.tool;

import com.hzj.aicodemother.constant.AppConstant;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/** 写入与读取工具共用的工程路径边界。 */
final class VueProjectFilePolicy {
    private VueProjectFilePolicy() { }

    static Path resolve(Long appId, String relativeFilePath, boolean write) {
        if (appId == null || appId <= 0 || relativeFilePath == null || relativeFilePath.isBlank()) {
            throw new IllegalArgumentException("工程文件参数不合法");
        }
        Path relative = Paths.get(relativeFilePath);
        if (relative.isAbsolute() || relativeFilePath.contains(":") || relativeFilePath.contains("\\")) {
            throw new IllegalArgumentException("只能使用工程内的正斜杠相对路径");
        }
        Path root = Paths.get(AppConstant.CODE_OUTPUT_ROOT_DIR, "vue_project_" + appId)
                .toAbsolutePath().normalize();
        Path path = root.resolve(relative).normalize();
        if (!path.startsWith(root) || path.equals(root)) {
            throw new IllegalArgumentException("文件路径超出工程目录");
        }
        String name = root.relativize(path).toString().replace('\\', '/');
        if (write && !isEditable(name)) {
            throw new IllegalArgumentException("此文件由平台模板维护，AI 不可修改: " + name);
        }
        if (!write && !isEditable(name) && !isProtectedReadable(name)) {
            throw new IllegalArgumentException("不允许读取此工程文件: " + name);
        }
        if (Files.isSymbolicLink(root) || !Files.isRegularFile(root.resolve(".platform-template"))) {
            throw new IllegalStateException("Vue 工程尚未初始化平台模板");
        }
        Path current = root;
        for (Path part : root.relativize(path)) {
            current = current.resolve(part);
            if (Files.isSymbolicLink(current)) {
                throw new IllegalArgumentException("不允许通过符号链接访问工程文件");
            }
        }
        return path;
    }

    private static boolean isEditable(String name) {
        if (name.equals("src/App.vue") || name.equals("src/router/index.js")) {
            return true;
        }
        boolean allowedDirectory = name.startsWith("src/pages/") || name.startsWith("src/components/")
                || name.startsWith("src/styles/") || name.startsWith("src/assets/")
                || name.startsWith("src/utils/") || name.startsWith("src/composables/")
                || name.startsWith("public/");
        return allowedDirectory && !name.contains("/.") && !name.endsWith("/")
                && (name.endsWith(".vue") || name.endsWith(".js") || name.endsWith(".css")
                || name.endsWith(".json") || name.endsWith(".svg") || name.endsWith(".txt"));
    }

    private static boolean isProtectedReadable(String name) {
        return name.equals("index.html") || name.equals("src/main.js") || name.equals("vite.config.js");
    }
}
