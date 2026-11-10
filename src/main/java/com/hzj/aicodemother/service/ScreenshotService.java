package com.hzj.aicodemother.service;

public interface ScreenshotService {
    String generateAndUploadScreenshot(String webUrl);

    void generatePreviewCoverAsync(Long appId, String codeGenType);
}
