package com.hzj.aicodemother.service;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AppAssetCleanupJob {
    private final AppAssetService appAssetService;

/**
 * 定时任务方法，用于清理过期的上传文件
 * 使用cron表达式设置执行时间：每小时30分执行一次
 *
 * @Scheduled 注解用于标记该方法为定时任务
 * cron表达式 "0 30 * * * *" 表示在每小时的30分整执行
 */
    @Scheduled(cron = "0 30 * * * *")
    public void cleanExpiredUploads() {
    // 调用服务方法清理未被引用的过期上传文件
        appAssetService.cleanupExpiredUnreferenced();
    }
}
