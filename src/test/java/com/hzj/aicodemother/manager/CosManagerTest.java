package com.hzj.aicodemother.manager;

import com.hzj.aicodemother.config.CosClientConfig;
import com.qcloud.cos.COSClient;
import com.qcloud.cos.model.CannedAccessControlList;
import com.qcloud.cos.model.PutObjectRequest;
import com.qcloud.cos.model.PutObjectResult;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.File;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;

class CosManagerTest {

    @Test
    void uploadsPngWithImageContentTypeAndAbsoluteUrl() {
        CosClientConfig config = new CosClientConfig();
        config.setBucket("test-bucket");
        config.setHost("https://test-bucket.cos.example.com/");
        COSClient client = mock(COSClient.class);
        when(client.putObject(any(PutObjectRequest.class))).thenReturn(new PutObjectResult());

        CosManager manager = new CosManager();
        ReflectionTestUtils.setField(manager, "cosClientConfig", config);
        ReflectionTestUtils.setField(manager, "cosClient", client);

        String url = manager.uploadPublicScreenshot("screenshots/2026/10/29/cover.png", new File("cover.png"));

        assertEquals("https://test-bucket.cos.example.com/screenshots/2026/10/29/cover.png", url);
        ArgumentCaptor<PutObjectRequest> request = ArgumentCaptor.forClass(PutObjectRequest.class);
        verify(client).putObject(request.capture());
        assertEquals("image/png", request.getValue().getMetadata().getContentType());
        assertEquals(CannedAccessControlList.PublicRead, request.getValue().getCannedAcl());
    }

    @Test
    void addsHttpsToExistingHostConfiguration() {
        CosClientConfig config = new CosClientConfig();
        config.setBucket("test-bucket");
        config.setHost("test-bucket.cos.example.com");
        COSClient client = mock(COSClient.class);
        when(client.putObject(any(PutObjectRequest.class))).thenReturn(new PutObjectResult());

        CosManager manager = new CosManager();
        ReflectionTestUtils.setField(manager, "cosClientConfig", config);
        ReflectionTestUtils.setField(manager, "cosClient", client);

        String url = manager.uploadFile("screenshots/cover.png", new File("cover.png"));

        assertEquals("https://test-bucket.cos.example.com/screenshots/cover.png", url);
        ArgumentCaptor<PutObjectRequest> request = ArgumentCaptor.forClass(PutObjectRequest.class);
        verify(client).putObject(request.capture());
        assertNull(request.getValue().getCannedAcl());
    }

    @Test
    void deletesOnlyOwnGeneratedScreenshot() {
        CosClientConfig config = new CosClientConfig();
        config.setBucket("test-bucket");
        config.setHost("https://test-bucket.cos.example.com");
        COSClient client = mock(COSClient.class);
        CosManager manager = new CosManager();
        ReflectionTestUtils.setField(manager, "cosClientConfig", config);
        ReflectionTestUtils.setField(manager, "cosClient", client);

        String key = "screenshots/2026/10/29/123e4567-e89b-12d3-a456-426614174000.png";
        assertFalse(manager.deleteGeneratedScreenshot("https://other.example.com/" + key));
        assertFalse(manager.deleteGeneratedScreenshot("https://test-bucket.cos.example.com/user/avatar.png"));
        verify(client, never()).deleteObject(any(String.class), any(String.class));
        assertTrue(manager.deleteGeneratedScreenshot("https://test-bucket.cos.example.com/" + key));
        verify(client).deleteObject("test-bucket", key);
    }
}
