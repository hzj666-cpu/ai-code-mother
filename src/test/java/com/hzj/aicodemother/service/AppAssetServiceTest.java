package com.hzj.aicodemother.service;

import com.hzj.aicodemother.core.builder.VueProjectTemplateService;
import com.hzj.aicodemother.manager.CosManager;
import com.hzj.aicodemother.mapper.AppMapper;
import com.mybatisflex.core.query.QueryWrapper;
import com.hzj.aicodemother.model.entity.AppAsset;
import com.hzj.aicodemother.model.dto.app.AppAssetVO;
import com.hzj.aicodemother.model.entity.App;
import com.hzj.aicodemother.model.entity.User;
import com.hzj.aicodemother.exception.BusinessException;
import dev.langchain4j.data.message.ImageContent;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.response.ChatResponse;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.web.MockMultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;

class AppAssetServiceTest {
    @Test
    void acceptsPngAndAssignsStableSitePath() throws Exception {
        AppMapper appMapper = mock(AppMapper.class);
        CosManager cosManager = mock(CosManager.class);
        AppAssetService service = spy(new AppAssetService(appMapper, cosManager,
                mock(ChatModel.class), mock(VueProjectTemplateService.class)));
        when(appMapper.selectOneById(42L)).thenReturn(App.builder().id(42L).userId(7L).build());
        doReturn(0L).when(service).count(any(QueryWrapper.class));
        doAnswer(invocation -> {
            AppAsset asset = invocation.getArgument(0);
            asset.setId(123L);
            return true;
        }).when(service).save(any(AppAsset.class));
        doReturn(true).when(service).updateById(any(AppAsset.class));
        MockMultipartFile image = new MockMultipartFile("file", "logo.png", "image/png", smallPng());

        AppAssetVO uploaded = service.upload(42L, User.builder().id(7L).build(), image, "SITE_ASSET");

        assertEquals("123", uploaded.getAssetId());
        assertEquals("assets/123.png", uploaded.getSitePath());
        verify(cosManager).putPrivateAsset(org.mockito.ArgumentMatchers.startsWith("app-assets/42/"),
                any(java.io.File.class));
    }

    @Test
    void rejectsUploadToAnotherUsersAppBeforeTouchingCos() {
        AppMapper appMapper = mock(AppMapper.class);
        CosManager cosManager = mock(CosManager.class);
        AppAssetService service = new AppAssetService(appMapper, cosManager,
                mock(ChatModel.class), mock(VueProjectTemplateService.class));
        when(appMapper.selectOneById(42L)).thenReturn(App.builder().id(42L).userId(8L).build());

        assertThrows(BusinessException.class,
                () -> service.upload(42L, User.builder().id(7L).build(), null, "REFERENCE"));
        verifyNoInteractions(cosManager);
    }

    @Test
    void sendsImageBytesToModelInsteadOfOnlyAnImageUrl() throws Exception {
        AppMapper appMapper = mock(AppMapper.class);
        CosManager cosManager = mock(CosManager.class);
        ChatModel model = mock(ChatModel.class);
        VueProjectTemplateService template = mock(VueProjectTemplateService.class);
        AppAssetService service = new AppAssetService(appMapper, cosManager, model, template);
        AppAsset asset = new AppAsset();
        asset.setStorageKey("app-assets/42/image.png");
        asset.setMimeType("image/png");
        asset.setPurpose("REFERENCE");
        when(cosManager.readAsset(asset.getStorageKey())).thenReturn(smallPng());
        ChatResponse response = mock(ChatResponse.class);
        when(response.aiMessage()).thenReturn(AiMessage.from("蓝色横幅，白色标题"));
        when(model.chat(any(UserMessage.class))).thenReturn(response);

        assertEquals("蓝色横幅，白色标题", service.analyze(List.of(asset), "修改首页"));

        ArgumentCaptor<UserMessage> sent = ArgumentCaptor.forClass(UserMessage.class);
        verify(model).chat(sent.capture());
        assertTrue(sent.getValue().contents().stream().anyMatch(ImageContent.class::isInstance));
    }

    private static byte[] smallPng() throws Exception {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        ImageIO.write(new BufferedImage(2, 2, BufferedImage.TYPE_INT_RGB), "png", output);
        return output.toByteArray();
    }
}
