package com.hzj.aicodemother.controller;


import com.hzj.aicodemother.constant.AppConstant;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.servlet.HandlerMapping;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

@RestController
@RequestMapping("/static")
public class StaticResourceController {

    // 应用生成根目录（用于浏览）
    //private static final String PREVIEW_ROOT_DIR = System.getProperty("user.dir") + "/tmp/code_output";
    // 应用生成根目录（用于浏览）
    private static final String PREVIEW_ROOT_DIR = AppConstant.CODE_OUTPUT_ROOT_DIR;

    @Value("${app.preview.allowed-hosts:localhost,127.0.0.1}")
    private String allowedHosts;

    /**
     * 提供静态资源访问，支持目录重定向
     * 访问格式：http://localhost:8123/api/static/{deployKey}[/{fileName}]
     */
    @GetMapping("/{deployKey}/**")
    public ResponseEntity<Resource> serveStaticResource(
            @PathVariable String deployKey,
            HttpServletRequest request) {
        try {
            if (allowedHosts != null && Arrays.stream(allowedHosts.split(","))
                    .map(String::trim).noneMatch(host -> host.equalsIgnoreCase(request.getServerName()))) {
                return ResponseEntity.notFound().build();
            }
            // 获取资源路径
            String resourcePath = (String) request.getAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE);
            resourcePath = resourcePath.substring(("/static/" + deployKey).length());
            // 如果是目录访问（不带斜杠），重定向到带斜杠的URL
            if (resourcePath.isEmpty()) {
                HttpHeaders headers = new HttpHeaders();
                headers.add("Location", request.getRequestURI() + "/");
                return new ResponseEntity<>(headers, HttpStatus.MOVED_PERMANENTLY);
            }
            // 默认返回 index.html
            if (resourcePath.equals("/")) {
                resourcePath = "/index.html";
            }
            // 构建文件路径
            if (!deployKey.matches("(?:html|multi_file|vue_project)_\\d+")) {
                return ResponseEntity.notFound().build();
            }
            Path root = Path.of(PREVIEW_ROOT_DIR).toAbsolutePath().normalize();
            Path appRoot = root.resolve(deployKey).normalize();
            Path target = appRoot.resolve(resourcePath.substring(1)).normalize();
            if (!target.startsWith(appRoot) || !Files.isRegularFile(target)
                    || Files.isSymbolicLink(appRoot) || Files.isSymbolicLink(target)) {
                return ResponseEntity.notFound().build();
            }
            Path current = appRoot;
            for (Path part : appRoot.relativize(target)) {
                current = current.resolve(part);
                if (Files.isSymbolicLink(current)) return ResponseEntity.notFound().build();
            }
            File file = target.toFile();
            // 检查文件是否存在
            if (!file.exists()) {
                return ResponseEntity.notFound().build();
            }
            // 返回文件资源
            Resource resource = new FileSystemResource(file);
            return ResponseEntity.ok()
                    .header("Content-Type", getContentTypeWithCharset(target.toString()))
                    .body(resource);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * 根据文件扩展名返回带字符编码的 Content-Type
     */
    private String getContentTypeWithCharset(String filePath) {
        if (filePath.endsWith(".html")) return "text/html; charset=UTF-8";
        if (filePath.endsWith(".css")) return "text/css; charset=UTF-8";
        if (filePath.endsWith(".js")) return "application/javascript; charset=UTF-8";
        if (filePath.endsWith(".png")) return "image/png";
        if (filePath.endsWith(".jpg")) return "image/jpeg";
        return "application/octet-stream";
    }
}
