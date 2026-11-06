package com.hzj.aicodemother.core.builder;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.core.io.ClassPathResource;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class VueTemplateBuildSmokeTest {
    @TempDir
    Path project;

    @Test
    void templateInstallsFromLockAndBuildsAtRelativeBase() throws Exception {
        String npm = System.getProperty("os.name").toLowerCase().contains("windows") ? "npm.cmd" : "npm";
        Process available = new ProcessBuilder("node", "--version").start();
        assumeTrue(available.waitFor(10, TimeUnit.SECONDS) && available.exitValue() == 0);

        Path source = new ClassPathResource("vue-templates/vue-base-v1").getFile().toPath();
        try (var files = Files.walk(source)) {
            for (Path file : files.filter(Files::isRegularFile).toList()) {
                Path target = project.resolve(source.relativize(file));
                Files.createDirectories(target.getParent());
                Files.copy(file, target);
            }
        }
        Process install = new ProcessBuilder(npm, "ci", "--ignore-scripts", "--no-audit", "--no-fund")
                .directory(project.toFile()).redirectErrorStream(true)
                .redirectOutput(project.resolve("install.log").toFile()).start();
        assertTrue(install.waitFor(180, TimeUnit.SECONDS) && install.exitValue() == 0,
                () -> "npm ci failed: " + readLog("install.log"));
        Process build = new ProcessBuilder("node", "node_modules/vite/bin/vite.js", "build",
                "--config", "vite.config.js")
                .directory(project.toFile()).redirectErrorStream(true)
                .redirectOutput(project.resolve("build.log").toFile()).start();
        assertTrue(build.waitFor(120, TimeUnit.SECONDS) && build.exitValue() == 0,
                () -> "vite build failed: " + readLog("build.log"));
        assertTrue(Files.readString(project.resolve("dist/index.html")).contains("./assets/"));
    }

    private String readLog(String file) {
        try { return Files.readString(project.resolve(file)); }
        catch (Exception e) { return e.getMessage(); }
    }
}
