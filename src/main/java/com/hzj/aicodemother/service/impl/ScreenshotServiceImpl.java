package com.hzj.aicodemother.service.impl;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import com.hzj.aicodemother.exception.ErrorCode;
import com.hzj.aicodemother.exception.ThrowUtils;
import com.hzj.aicodemother.manager.CosManager;
import com.hzj.aicodemother.mapper.AppMapper;
import com.hzj.aicodemother.model.entity.App;
import com.hzj.aicodemother.service.ScreenshotService;
import com.hzj.aicodemother.util.WebScreenshotUtils;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;

@Service
@Slf4j
public class ScreenshotServiceImpl implements ScreenshotService {

    @Resource
    private CosManager cosManager;

    @Resource
    private AppMapper appMapper;

    @Resource
    private Executor screenshotTaskExecutor;

    @Value("${server.port:8124}")
    private int serverPort;

    @Value("${server.servlet.context-path:/api}")
    private String contextPath;

    @Override
    public void generatePreviewCoverAsync(Long appId, String codeGenType) {
        App app = appMapper.selectOneById(appId);
        if (app == null || StrUtil.isNotBlank(app.getCover())) {
            return;
        }
        String previewPath = "vue_project".equals(codeGenType) ? "dist/index.html" : "";
        String url = String.format("http://127.0.0.1:%d%s/static/%s_%d/%s",
                serverPort, contextPath, codeGenType, appId, previewPath);
        try {
            screenshotTaskExecutor.execute(() -> {
                String screenshotUrl = null;
                try {
                    screenshotUrl = generateAndUploadScreenshot(url);
                    if (appMapper.setCoverIfMissing(appId, screenshotUrl) != 1) {
                        cosManager.deleteGeneratedScreenshot(screenshotUrl);
                    } else {
                        log.info("已生成应用预览封面, appId={}", appId);
                    }
                } catch (Exception e) {
                    log.error("应用预览封面生成失败, appId={}, url={}", appId, url, e);
                    if (screenshotUrl != null) {
                        try {
                            cosManager.deleteGeneratedScreenshot(screenshotUrl);
                        } catch (Exception cleanupError) {
                            log.warn("清理未关联的预览封面失败, appId={}", appId, cleanupError);
                        }
                    }
                }
            });
        } catch (RejectedExecutionException e) {
            log.warn("应用预览封面任务队列已满, appId={}", appId, e);
        }
    }

    @Override
    public String generateAndUploadScreenshot(String webUrl) {
        ThrowUtils.throwIf(StrUtil.isBlank(webUrl), ErrorCode.PARAMS_ERROR, "网页URL不能为空");
        log.info("开始生成网页截图，URL: {}", webUrl);
        // 1. 生成本地截图
        String localScreenshotPath = WebScreenshotUtils.saveWebPageScreenshot(webUrl);
        ThrowUtils.throwIf(StrUtil.isBlank(localScreenshotPath), ErrorCode.OPERATION_ERROR, "本地截图生成失败");
        try {
            // 2. 上传到对象存储
            String cosUrl = uploadScreenshotToCos(localScreenshotPath);
            ThrowUtils.throwIf(StrUtil.isBlank(cosUrl), ErrorCode.OPERATION_ERROR, "截图上传对象存储失败");
            log.info("网页截图生成并上传成功: {} -> {}", webUrl, cosUrl);
            return cosUrl;
        } finally {
            // 3. 清理本地文件
            cleanupLocalFile(localScreenshotPath);
        }
    }

    /**
     * 上传截图到对象存储
     *
     * @param localScreenshotPath 本地截图路径
     * @return 对象存储访问URL，失败返回null
     */
    private String uploadScreenshotToCos(String localScreenshotPath) {
        if (StrUtil.isBlank(localScreenshotPath)) {
            return null;
        }
        File screenshotFile = new File(localScreenshotPath);
        if (!screenshotFile.exists()) {
            log.error("截图文件不存在: {}", localScreenshotPath);
            return null;
        }
        // 生成 COS 对象键
        String fileName = UUID.randomUUID() + ".png";
        String cosKey = generateScreenshotKey(fileName);
        return cosManager.uploadPublicScreenshot(cosKey, screenshotFile);
    }

    /**
     * 生成截图的对象存储键
     * 格式：screenshots/yyyy/MM/dd/filename.png
     */
    private String generateScreenshotKey(String fileName) {
        String datePath = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
        return String.format("screenshots/%s/%s", datePath, fileName);
    }

    /**
     * 清理本地文件
     *
     * @param localFilePath 本地文件路径
     */
    private void cleanupLocalFile(String localFilePath) {
        File localFile = new File(localFilePath);
        if (localFile.exists()) {
            File parentDir = localFile.getParentFile();
            FileUtil.del(parentDir);
            log.info("本地截图文件已清理: {}", localFilePath);
        }
    }
}
