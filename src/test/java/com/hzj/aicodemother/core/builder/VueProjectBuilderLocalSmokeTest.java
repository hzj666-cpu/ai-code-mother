package com.hzj.aicodemother.core.builder;

import cn.hutool.core.io.FileUtil;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class VueProjectBuilderLocalSmokeTest {
    @Test
    void trustedTemplateBuildsThroughRunner() {
        long appId = System.nanoTime();
        VueProjectTemplateService templates = new VueProjectTemplateService();
        Path project = templates.projectDir(appId);
        try {
            templates.ensureInitialized(appId, VueProjectTemplateService.VERSION);
            VueProjectBuilder builder = new VueProjectBuilder();
            ReflectionTestUtils.setField(builder, "templateService", templates);
            ReflectionTestUtils.setField(builder, "runner", "local");
            builder.buildProject(appId);
            assertTrue(Files.isRegularFile(project.resolve("dist/index.html")));
        } finally {
            if (Files.exists(project)) FileUtil.del(project.toFile());
        }
    }
}
