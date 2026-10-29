package com.hzj.aicodemother.service.impl;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ScreenshotCleanupJobTest {
    @TempDir
    Path root;

    @Test
    void removesOnlyExpiredCaptureDirectories() throws Exception {
        Path expired = Files.createDirectory(root.resolve("capture-old"));
        Files.writeString(expired.resolve("screenshot.png"), "old");
        Files.setLastModifiedTime(expired, FileTime.from(Instant.now().minusSeconds(90000)));
        Path fresh = Files.createDirectory(root.resolve("capture-new"));
        Files.writeString(fresh.resolve("screenshot.png"), "new");
        Path unrelated = Files.createDirectory(root.resolve("other"));
        Files.setLastModifiedTime(unrelated, FileTime.from(Instant.now().minusSeconds(90000)));

        new ScreenshotCleanupJob().cleanExpiredScreenshots(root, Instant.now().minusSeconds(86400));

        assertFalse(Files.exists(expired));
        assertTrue(Files.exists(fresh.resolve("screenshot.png")));
        assertTrue(Files.exists(unrelated));
    }
}
