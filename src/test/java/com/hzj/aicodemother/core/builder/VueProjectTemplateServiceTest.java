package com.hzj.aicodemother.core.builder;

import cn.hutool.core.io.FileUtil;
import com.hzj.aicodemother.ai.tool.FileWriteTool;
import com.hzj.aicodemother.ai.tool.ReadProjectFileTool;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class VueProjectTemplateServiceTest {
    @Test
    void initializesOnceAndProtectsPlatformFiles() throws Exception {
        long appId = System.nanoTime();
        VueProjectTemplateService templates = new VueProjectTemplateService();
        Path project = templates.projectDir(appId);
        try {
            templates.ensureInitialized(appId, VueProjectTemplateService.VERSION);
            assertTrue(Files.isRegularFile(project.resolve("package-lock.json")));
            assertThrows(IllegalArgumentException.class,
                    () -> new FileWriteTool().writeFile("package.json", "{}", appId));
            new FileWriteTool().writeFile("src/App.vue", "<template>updated</template>", appId);
            assertEquals("<template>updated</template>",
                    new ReadProjectFileTool().readFile("src/App.vue", appId));
            templates.ensureInitialized(appId, VueProjectTemplateService.VERSION);
            assertEquals("<template>updated</template>", Files.readString(project.resolve("src/App.vue")));
            assertThrows(IllegalStateException.class, () -> templates.ensureInitialized(appId, null));
            Files.writeString(project.resolve("vite.config.js"), "unsafe");
            assertThrows(IllegalStateException.class, () -> templates.verifyProject(project));
        } finally {
            if (Files.exists(project)) FileUtil.del(project.toFile());
        }
    }
}
