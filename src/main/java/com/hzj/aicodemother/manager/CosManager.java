package com.hzj.aicodemother.manager;

import com.hzj.aicodemother.config.CosClientConfig;
import com.qcloud.cos.COSClient;
import com.qcloud.cos.model.CannedAccessControlList;
import com.qcloud.cos.model.ObjectMetadata;
import com.qcloud.cos.model.PutObjectRequest;
import com.qcloud.cos.model.PutObjectResult;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.File;
import java.net.URI;
import java.net.URLConnection;

/**
 * COS对象存储管理器
 *
 * @author yupi
 */
@Component
@Slf4j
public class CosManager {

    @Resource
    private CosClientConfig cosClientConfig;

    @Resource
    private COSClient cosClient;

    /**
     * 上传对象
     *
     * @param key  唯一键
     * @param file 文件
     * @return 上传结果
     */
    public PutObjectResult putObject(String key, File file) {
        return putObject(key, file, null);
    }

    private PutObjectResult putObject(String key, File file, CannedAccessControlList acl) {
        PutObjectRequest putObjectRequest = new PutObjectRequest(cosClientConfig.getBucket(), key, file);
        if (acl != null) {
            putObjectRequest.setCannedAcl(acl);
        }
        String contentType = URLConnection.guessContentTypeFromName(key);
        if (contentType != null) {
            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentType(contentType);
            putObjectRequest.setMetadata(metadata);
        }
        return cosClient.putObject(putObjectRequest);
    }

    /**
     * 上传文件到 COS 并返回访问 URL
     *
     * @param key  COS对象键（完整路径）
     * @param file 要上传的文件
     * @return 文件的访问URL，失败返回null
     */
    public String uploadFile(String key, File file) {
        return uploadFile(key, file, null);
    }

    /** 仅应用自动封面使用，其他文件沿用存储桶默认访问权限。 */
    public String uploadPublicScreenshot(String key, File file) {
        return uploadFile(key, file, CannedAccessControlList.PublicRead);
    }

    /** 只删除当前 COS 域名下由截图服务生成的对象。 */
    public boolean deleteGeneratedScreenshot(String coverUrl) {
        if (coverUrl == null || coverUrl.isBlank()) {
            return false;
        }
        URI baseUri = URI.create(cosClientConfig.getHost().contains("://")
                ? cosClientConfig.getHost() : "https://" + cosClientConfig.getHost());
        URI coverUri;
        try {
            coverUri = URI.create(coverUrl);
        } catch (IllegalArgumentException e) {
            return false;
        }
        if (!"https".equalsIgnoreCase(coverUri.getScheme())
                || !baseUri.getHost().equalsIgnoreCase(coverUri.getHost())
                || coverUri.getPort() != baseUri.getPort()
                || coverUri.getRawQuery() != null || coverUri.getRawFragment() != null) {
            return false;
        }
        String path = coverUri.getPath();
        if (path == null || !path.matches("/screenshots/\\d{4}/\\d{2}/\\d{2}/[0-9a-fA-F-]{36}\\.png")) {
            return false;
        }
        cosClient.deleteObject(cosClientConfig.getBucket(), path.substring(1));
        return true;
    }

    private String uploadFile(String key, File file, CannedAccessControlList acl) {
        String host = cosClientConfig.getHost().trim();
        URI baseUri = URI.create(host.contains("://") ? host : "https://" + host);
        if (!"https".equalsIgnoreCase(baseUri.getScheme()) || baseUri.getHost() == null) {
            throw new IllegalArgumentException("COS 访问域名必须是完整的 HTTPS URL");
        }
        String normalizedKey = key.startsWith("/") ? key.substring(1) : key;
        // 上传文件
        PutObjectResult result = putObject(key, file, acl);
        if (result != null) {
            // 构建访问URL
            String url = baseUri.resolve("/" + normalizedKey).toString();
            log.info("文件上传COS成功: {} -> {}", file.getName(), url);
            return url;
        } else {
            log.error("文件上传COS失败，返回结果为空");
            return null;
        }
    }
}
