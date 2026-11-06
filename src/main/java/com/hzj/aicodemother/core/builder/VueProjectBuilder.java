package com.hzj.aicodemother.core.builder;

import cn.hutool.core.io.FileUtil;
import com.hzj.aicodemother.mapper.AppMapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.nio.charset.StandardCharsets;
import java.nio.file.attribute.PosixFilePermission;
import java.time.Duration;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.UUID;
import java.util.Set;

/** 平台模板的受控构建入口。 */
@Slf4j
@Component
public class VueProjectBuilder {
    @Resource
    private VueProjectTemplateService templateService;
    @Resource
    private AppMapper appMapper;
    @Value("${app.vue-build.runner:docker}")
    private String runner;
    @Value("${app.vue-build.image:ai-code-mother/vue-builder:v1}")
    private String image;

    private final ThreadPoolExecutor executor = new ThreadPoolExecutor(1, 1, 0, TimeUnit.SECONDS,
            new ArrayBlockingQueue<>(20), task -> {
                Thread thread = new Thread(task, "vue-project-builder");
                thread.setDaemon(true);
                return thread;
            }, new ThreadPoolExecutor.AbortPolicy());

    public String beginGeneration(Long appId) {
        String token = UUID.randomUUID().toString();
        if (appMapper.beginBuild(appId, token, "PENDING", "") != 1) {
            throw new IllegalStateException("应用不存在，无法开始生成");
        }
        return token;
    }

    public void buildProjectAsync(Long appId, String token) {
        try {
            executor.execute(() -> {
                if (updateStatus(appId, token, "PENDING", "BUILDING", "") != 1) return;
                try {
                    buildProject(appId);
                    updateStatus(appId, token, "BUILDING", "SUCCESS", "");
                } catch (Exception e) {
                    log.error("Vue 工程构建失败, appId={}", appId, e);
                    updateStatus(appId, token, "BUILDING", "FAILED", "构建失败，请检查生成代码与构建日志");
                }
            });
        } catch (RuntimeException e) {
            updateStatus(appId, token, "PENDING", "FAILED", "构建任务队列已满，请稍后重试");
            throw e;
        }
    }

    public void failGeneration(Long appId, String token) {
        updateStatus(appId, token, "PENDING", "FAILED", "代码生成失败，请重试");
    }

    /** 只接受受管模板；部署直接复用成功构建产物。 */
    public void buildProject(Long appId) {
        Path project = templateService.projectDir(appId);
        templateService.verifyProject(project);
        try {
            Files.writeString(project.resolve(".platform-build.log"), "", StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING);
            if ("docker".equals(runner)) {
                runDockerBuild(project);
            } else if ("local".equals(runner)) {
                log.warn("Vue 工程正在本机直接构建，仅限可信开发环境, appId={}", appId);
                run(project, Duration.ofMinutes(5), npmCommand(), "ci", "--ignore-scripts", "--no-audit", "--no-fund");
                run(project, Duration.ofMinutes(3), "node", "node_modules/vite/bin/vite.js", "build", "--config", "vite.config.js");
            } else {
                throw new IllegalStateException("不支持的 Vue 构建运行器: " + runner);
            }
            if (!Files.isRegularFile(project.resolve("dist/index.html"))) {
                throw new IllegalStateException("Vue 构建未生成 dist/index.html");
            }
        } catch (IOException e) {
            throw new IllegalStateException("Vue 工程构建失败", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Vue 工程构建被中断", e);
        }
    }

    private void runDockerBuild(Path project) throws IOException, InterruptedException {
        Path staged = Files.createTempDirectory(project.getParent(), "vue-dist-");
        try {
            String containerUser = "10001:10001";
            try {
                int uid = (int) Files.getAttribute(staged, "unix:uid");
                int gid = (int) Files.getAttribute(staged, "unix:gid");
                if (uid > 0) {
                    containerUser = uid + ":" + gid;
                } else {
                    Files.setPosixFilePermissions(staged, Set.of(PosixFilePermission.OWNER_READ,
                            PosixFilePermission.OWNER_WRITE, PosixFilePermission.OWNER_EXECUTE,
                            PosixFilePermission.GROUP_READ, PosixFilePermission.GROUP_WRITE,
                            PosixFilePermission.GROUP_EXECUTE, PosixFilePermission.OTHERS_READ,
                            PosixFilePermission.OTHERS_WRITE, PosixFilePermission.OTHERS_EXECUTE));
                }
            } catch (UnsupportedOperationException ignored) {
                // Docker Desktop 的 Windows 挂载不使用 POSIX 文件权限。
            }
            run(project, Duration.ofMinutes(4), "docker", "run", "--rm", "--network", "none",
                    "--user", containerUser, "--read-only", "--cap-drop", "ALL", "--security-opt", "no-new-privileges",
                    "--pids-limit", "128", "--memory", "512m", "--cpus", "1",
                    "--tmpfs", "/tmp:rw,nosuid,noexec,size=64m",
                    "--tmpfs", "/work:rw,nosuid,exec,size=512m",
                    "--mount", "type=bind,source=" + project + ",target=/input,readonly",
                    "--mount", "type=bind,source=" + staged + ",target=/output",
                    image);
            if (!Files.isRegularFile(staged.resolve("index.html"))) {
                throw new IllegalStateException("隔离构建未生成 dist/index.html");
            }
            Path dist = project.resolve("dist");
            Path backup = project.resolve(".dist-backup-" + UUID.randomUUID());
            if (Files.exists(dist)) Files.move(dist, backup);
            try {
                Files.move(staged, dist);
            } catch (IOException e) {
                if (Files.exists(backup)) Files.move(backup, dist);
                throw e;
            }
            try {
                Files.setPosixFilePermissions(dist, Set.of(PosixFilePermission.OWNER_READ,
                        PosixFilePermission.OWNER_WRITE, PosixFilePermission.OWNER_EXECUTE,
                        PosixFilePermission.GROUP_READ, PosixFilePermission.GROUP_EXECUTE,
                        PosixFilePermission.OTHERS_READ, PosixFilePermission.OTHERS_EXECUTE));
            } catch (UnsupportedOperationException ignored) {
                // Windows 挂载不使用 POSIX 文件权限。
            }
            if (Files.exists(backup)) FileUtil.del(backup.toFile());
        } finally {
            if (Files.exists(staged)) FileUtil.del(staged.toFile());
        }
    }

    private int updateStatus(Long appId, String token, String expectedStatus, String status, String error) {
        return appMapper.updateBuildStatus(appId, token, expectedStatus, status, error);
    }

    private String npmCommand() {
        return System.getProperty("os.name").toLowerCase().contains("windows") ? "npm.cmd" : "npm";
    }

    private void run(Path project, Duration timeout, String... command) throws IOException, InterruptedException {
        Path logFile = project.resolve(".platform-build.log");
        ProcessBuilder builder = new ProcessBuilder(command);
        builder.directory(project.toFile()).redirectErrorStream(true);
        String path = builder.environment().getOrDefault("PATH", builder.environment().get("Path"));
        String systemRoot = builder.environment().get("SystemRoot");
        String homeDirectory = builder.environment().get("HOME");
        String userProfile = builder.environment().get("USERPROFILE");
        String dockerConfig = builder.environment().get("DOCKER_CONFIG");
        String dockerHost = builder.environment().get("DOCKER_HOST");
        String dockerContext = builder.environment().get("DOCKER_CONTEXT");
        builder.environment().clear();
        if (path != null) builder.environment().put("PATH", path);
        if (systemRoot != null) builder.environment().put("SystemRoot", systemRoot);
        builder.environment().put("HOME", project.toString());
        builder.environment().put("USERPROFILE", project.toString());
        if ("docker".equals(command[0])) {
            if (homeDirectory != null) builder.environment().put("HOME", homeDirectory);
            if (userProfile != null) builder.environment().put("USERPROFILE", userProfile);
            if (dockerConfig != null) builder.environment().put("DOCKER_CONFIG", dockerConfig);
            if (dockerHost != null) builder.environment().put("DOCKER_HOST", dockerHost);
            if (dockerContext != null) builder.environment().put("DOCKER_CONTEXT", dockerContext);
        }
        builder.environment().put("CI", "true");
        Process process = builder.start();
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        Thread reader = Thread.ofVirtual().start(() -> {
            try (InputStream stream = process.getInputStream()) {
                byte[] buffer = new byte[8192];
                int read;
                while ((read = stream.read(buffer)) != -1) {
                    synchronized (output) {
                        int remaining = 16 * 1024 - output.size();
                        if (remaining > 0) output.write(buffer, 0, Math.min(read, remaining));
                    }
                }
            } catch (IOException e) {
                log.debug("读取构建进程输出结束", e);
            }
        });
        if (!process.waitFor(timeout.toSeconds(), TimeUnit.SECONDS)) {
            process.descendants().forEach(child -> child.destroyForcibly());
            process.destroyForcibly();
            reader.join(2000);
            throw new IllegalStateException("构建命令超时: " + command[0]);
        }
        reader.join(2000);
        Files.write(logFile, output.toByteArray(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        if (process.exitValue() != 0) {
            log.warn("Vue 构建命令失败, command={}, output={}", command[0], output.toString(StandardCharsets.UTF_8));
            throw new IllegalStateException("构建命令失败: " + command[0] + "，退出码=" + process.exitValue());
        }
    }
}
