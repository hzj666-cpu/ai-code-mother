package com.hzj.aicodemother.service.impl;

import com.hzj.aicodemother.manager.CosManager;
import com.hzj.aicodemother.mapper.AppMapper;
import com.hzj.aicodemother.model.entity.App;
import com.hzj.aicodemother.util.WebScreenshotUtils;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.Executor;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class ScreenshotServiceImplTest {

    @Test
    void createsMissingCoverFromLocalPreviewWithoutDeployment() {
        ScreenshotServiceImpl service = spy(new ScreenshotServiceImpl());
        AppMapper appMapper = mock(AppMapper.class);
        CosManager cosManager = mock(CosManager.class);
        ReflectionTestUtils.setField(service, "appMapper", appMapper);
        ReflectionTestUtils.setField(service, "cosManager", cosManager);
        ReflectionTestUtils.setField(service, "screenshotTaskExecutor", (Executor) Runnable::run);
        ReflectionTestUtils.setField(service, "serverPort", 8124);
        ReflectionTestUtils.setField(service, "contextPath", "/api");
        when(appMapper.selectOneById(42L)).thenReturn(new App());
        when(appMapper.setCoverIfMissing(42L, "https://cos.example.com/cover.png")).thenReturn(1);
        doReturn("https://cos.example.com/cover.png").when(service)
                .generateAndUploadScreenshot(anyString());

        service.generatePreviewCoverAsync(42L, "html");
        verify(service).generateAndUploadScreenshot("http://127.0.0.1:8124/api/static/html_42/");
        service.generatePreviewCoverAsync(42L, "vue_project");
        verify(service).generateAndUploadScreenshot("http://127.0.0.1:8124/api/static/vue_project_42/dist/index.html");
        verify(appMapper, times(2)).setCoverIfMissing(42L, "https://cos.example.com/cover.png");
        verifyNoInteractions(cosManager);
    }

    @Test
    void uploadsPngKeyAndRemovesTemporaryDirectory() throws Exception {
        Path directory = Files.createTempDirectory("screenshot-service-");
        Path screenshot = directory.resolve("screenshot.png");
        Files.write(screenshot, new byte[]{1, 2, 3});
        CosManager cosManager = mock(CosManager.class);
        when(cosManager.uploadPublicScreenshot(anyString(), eq(screenshot.toFile())))
                .thenReturn("https://cos.example.com/screenshots/cover.png");
        ScreenshotServiceImpl service = new ScreenshotServiceImpl();
        ReflectionTestUtils.setField(service, "cosManager", cosManager);

        try (MockedStatic<WebScreenshotUtils> screenshots = mockStatic(WebScreenshotUtils.class)) {
            screenshots.when(() -> WebScreenshotUtils.saveWebPageScreenshot("http://localhost/app/"))
                    .thenReturn(screenshot.toString());
            assertEquals("https://cos.example.com/screenshots/cover.png",
                    service.generateAndUploadScreenshot("http://localhost/app/"));
            assertFalse(Files.exists(directory));
        } finally {
            if (Files.exists(directory)) {
                Files.deleteIfExists(screenshot);
                Files.deleteIfExists(directory);
            }
        }

        ArgumentCaptor<String> key = ArgumentCaptor.forClass(String.class);
        verify(cosManager).uploadPublicScreenshot(key.capture(), eq(screenshot.toFile()));
        assertTrue(key.getValue().matches("screenshots/\\d{4}/\\d{2}/\\d{2}/[0-9a-f-]+\\.png"));
    }
}
