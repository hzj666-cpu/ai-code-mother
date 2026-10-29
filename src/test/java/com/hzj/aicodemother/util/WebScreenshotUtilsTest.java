package com.hzj.aicodemother.util;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class WebScreenshotUtilsTest {

    @Test
    void screenshotsLocalPageAsPng() throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/app/", exchange -> {
            byte[] html = "<html><body><h1>Screenshot test app</h1></body></html>"
                    .getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8");
            exchange.sendResponseHeaders(200, html.length);
            try (var body = exchange.getResponseBody()) {
                body.write(html);
            }
        });
        server.start();

        Path screenshot = null;
        try {
            String url = "http://127.0.0.1:" + server.getAddress().getPort() + "/app/";
            String result = WebScreenshotUtils.saveWebPageScreenshot(url);
            assertNotNull(result);
            screenshot = Path.of(result);
            assertTrue(Files.isRegularFile(screenshot));
            assertTrue(screenshot.getFileName().toString().endsWith(".png"));
            assertNotNull(ImageIO.read(screenshot.toFile()));
        } finally {
            if (screenshot != null) {
                Files.deleteIfExists(screenshot);
                Files.deleteIfExists(screenshot.getParent());
            }
            server.stop(0);
        }
    }

    @Test
    void concurrentScreenshotsUseSeparateBrowserPages() throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/red/", exchange -> writeColorPage(exchange, "#ff0000"));
        server.createContext("/blue/", exchange -> writeColorPage(exchange, "#0000ff"));
        server.start();
        var workers = Executors.newFixedThreadPool(2);
        Path redScreenshot = null;
        Path blueScreenshot = null;
        try {
            String base = "http://127.0.0.1:" + server.getAddress().getPort();
            var redResult = workers.submit(() -> WebScreenshotUtils.saveWebPageScreenshot(base + "/red/"));
            var blueResult = workers.submit(() -> WebScreenshotUtils.saveWebPageScreenshot(base + "/blue/"));
            assertNotNull(redResult.get(90, TimeUnit.SECONDS));
            assertNotNull(blueResult.get(90, TimeUnit.SECONDS));
            redScreenshot = Path.of(redResult.get());
            blueScreenshot = Path.of(blueResult.get());
            assertEquals(Color.RED.getRGB(), ImageIO.read(redScreenshot.toFile()).getRGB(20, 20));
            assertEquals(Color.BLUE.getRGB(), ImageIO.read(blueScreenshot.toFile()).getRGB(20, 20));
        } finally {
            deleteScreenshot(redScreenshot);
            deleteScreenshot(blueScreenshot);
            workers.shutdownNow();
            server.stop(0);
        }
    }

    private static void writeColorPage(com.sun.net.httpserver.HttpExchange exchange, String color)
            throws java.io.IOException {
        byte[] html = ("<html><style>html,body{margin:0;width:100%;height:100%;background:"
                + color + "}</style><body></body></html>").getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8");
        exchange.sendResponseHeaders(200, html.length);
        try (var body = exchange.getResponseBody()) {
            body.write(html);
        }
    }

    private static void deleteScreenshot(Path screenshot) throws Exception {
        if (screenshot != null) {
            Files.deleteIfExists(screenshot);
            Files.deleteIfExists(screenshot.getParent());
        }
    }
}
