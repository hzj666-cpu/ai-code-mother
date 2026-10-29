package com.hzj.aicodemother.util;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;

@Slf4j
public class WebScreenshotUtils {

    private static final int DEFAULT_WIDTH = 1600;
    private static final int DEFAULT_HEIGHT = 900;

    private WebScreenshotUtils() {
    }

    public static String saveWebPageScreenshot(String webUrl) {
        if (StrUtil.isBlank(webUrl)) {
            log.error("网页截图失败，网页地址为空");
            return null;
        }

        WebDriver driver = null;
        Path screenshotDir = null;
        try {
            driver = initChromeDriver();
            driver.manage().timeouts().pageLoadTimeout(Duration.ofSeconds(30));
            driver.get(webUrl);
            waitForPageLoad(driver);

            Path root = Path.of(System.getProperty("user.dir"), "tmp", "screenshot");
            Files.createDirectories(root);
            screenshotDir = Files.createTempDirectory(root, "capture-");
            Path screenshot = screenshotDir.resolve("screenshot.png");
            byte[] pngBytes = ((TakesScreenshot) driver).getScreenshotAs(OutputType.BYTES);
            Files.write(screenshot, pngBytes);
            log.info("网页截图已保存: {}", screenshot);
            return screenshot.toString();
        } catch (Exception e) {
            log.error("网页截图失败: {}", webUrl, e);
            if (screenshotDir != null) {
                FileUtil.del(screenshotDir.toFile());
            }
            return null;
        } finally {
            if (driver != null) {
                try {
                    driver.quit();
                } catch (Exception e) {
                    log.warn("关闭截图浏览器失败", e);
                }
            }
        }
    }

    private static WebDriver initChromeDriver() {
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--headless");
        options.addArguments("--disable-gpu");
        options.addArguments("--no-sandbox");
        options.addArguments("--disable-dev-shm-usage");
        options.addArguments(String.format("--window-size=%d,%d", DEFAULT_WIDTH, DEFAULT_HEIGHT));
        options.addArguments("--disable-extensions");
        return new ChromeDriver(options);
    }

    private static void waitForPageLoad(WebDriver driver) throws IOException {
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(30));
        wait.until(current -> "complete".equals(
                ((JavascriptExecutor) current).executeScript("return document.readyState")));
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("等待页面加载被中断", e);
        }
    }
}
