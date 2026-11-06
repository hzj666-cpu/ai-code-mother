package com.hzj.aicodemother.controller;

import cn.hutool.core.io.FileUtil;
import com.hzj.aicodemother.constant.AppConstant;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;

class StaticResourceControllerTest {
    @Test
    void servesOnlyFilesWithinNamedProject() throws Exception {
        String key = "vue_project_" + System.nanoTime();
        Path project = Path.of(AppConstant.CODE_OUTPUT_ROOT_DIR, key);
        try {
            Files.createDirectories(project);
            Files.writeString(project.resolve("index.html"), "safe");
            StaticResourceController controller = new StaticResourceController();
            ReflectionTestUtils.setField(controller, "allowedHosts", "preview.example.com");
            MockHttpServletRequest valid = new MockHttpServletRequest();
            valid.setServerName("preview.example.com");
            valid.setAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE,
                    "/static/" + key + "/index.html");
            assertEquals(200, controller.serveStaticResource(key, valid).getStatusCode().value());
            valid.setServerName("admin.example.com");
            assertEquals(404, controller.serveStaticResource(key, valid).getStatusCode().value());
            MockHttpServletRequest traversal = new MockHttpServletRequest();
            traversal.setServerName("preview.example.com");
            traversal.setAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE,
                    "/static/" + key + "/../other.txt");
            assertEquals(404, controller.serveStaticResource(key, traversal).getStatusCode().value());
        } finally {
            if (Files.exists(project)) FileUtil.del(project.toFile());
        }
    }
}
