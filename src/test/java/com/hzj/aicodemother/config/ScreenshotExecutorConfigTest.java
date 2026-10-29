package com.hzj.aicodemother.config;

import org.junit.jupiter.api.Test;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ScreenshotExecutorConfigTest {

    @Test
    void rejectsTasksAfterTwoWorkersAndTwentyQueuedJobs() throws Exception {
        ThreadPoolTaskExecutor executor = new ScreenshotExecutorConfig().screenshotTaskExecutor();
        executor.initialize();
        CountDownLatch started = new CountDownLatch(2);
        CountDownLatch release = new CountDownLatch(1);
        try {
            for (int i = 0; i < 2; i++) {
                executor.execute(() -> {
                    started.countDown();
                    await(release);
                });
            }
            assertTrue(started.await(5, TimeUnit.SECONDS));
            for (int i = 0; i < 20; i++) {
                executor.execute(() -> await(release));
            }
            assertThrows(RejectedExecutionException.class,
                    () -> executor.execute(() -> await(release)));
        } finally {
            release.countDown();
            executor.shutdown();
        }
    }

    private static void await(CountDownLatch latch) {
        try {
            latch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
