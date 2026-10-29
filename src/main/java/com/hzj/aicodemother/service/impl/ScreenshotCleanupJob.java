package com.hzj.aicodemother.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.FileVisitResult;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.Duration;
import java.time.Instant;

/** 清理进程中断后遗留的本地截图目录。 */
@Component
@Slf4j
public class ScreenshotCleanupJob {

    private static final Duration RETENTION = Duration.ofHours(24);

    @Scheduled(cron = "0 0 3 * * *")
    public void cleanExpiredScreenshots() {
        Path root = Path.of(System.getProperty("user.dir"), "tmp", "screenshot");
        try {
            cleanExpiredScreenshots(root, Instant.now().minus(RETENTION));
        } catch (IOException e) {
            log.warn("清理本地截图目录失败, root={}", root, e);
        }
    }

    void cleanExpiredScreenshots(Path root, Instant cutoff) throws IOException {
        if (!Files.isDirectory(root) || Files.isSymbolicLink(root)) {
            return;
        }
        try (DirectoryStream<Path> children = Files.newDirectoryStream(root, "capture-*")) {
            for (Path child : children) {
                if (!Files.isDirectory(child) || Files.isSymbolicLink(child)
                        || !Files.getLastModifiedTime(child).toInstant().isBefore(cutoff)) {
                    continue;
                }
                try {
                    Files.walkFileTree(child, new SimpleFileVisitor<>() {
                        @Override
                        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                            Files.delete(file);
                            return FileVisitResult.CONTINUE;
                        }

                        @Override
                        public FileVisitResult postVisitDirectory(Path dir, IOException error) throws IOException {
                            if (error != null) {
                                throw error;
                            }
                            Files.delete(dir);
                            return FileVisitResult.CONTINUE;
                        }
                    });
                    log.info("已清理过期截图目录: {}", child);
                } catch (IOException e) {
                    log.warn("清理过期截图目录失败: {}", child, e);
                }
            }
        }
    }
}
