package com.hzj.aicodemother.core;

import com.hzj.aicodemother.ai.model.enums.CodeGenTypeEnum;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.File;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** 真实调用模型并写入本地文件；需要手动开启且依赖 Redis、MySQL 和模型配置。 */
@SpringBootTest
@EnabledIfEnvironmentVariable(named = "RUN_LIVE_VUE_GENERATION_TEST", matches = "true")
class VueCodeGenerationIntegrationTest {

    @Resource
    private AiCodeGeneratorFacade aiCodeGeneratorFacade;

    @Test
    void generateVueProjectCodeStream() {
        // Each live run uses a fresh workspace and chat memory unless a specific appId is supplied.
        Long appId = Long.getLong("vue.test.appId", System.currentTimeMillis());
        AtomicReference<File> savedDir = new AtomicReference<>();

        List<String> chunks = aiCodeGeneratorFacade.generateAndSaveCodeStream(
                        "简单的任务记录网站，总代码量不超过 200 行",
                        CodeGenTypeEnum.VUE_PROJECT,
                        savedDir::set,
                        appId)
                .collectList()
                .block(Duration.ofMinutes(15));

        assertNotNull(chunks);
        assertFalse(chunks.isEmpty(), "模型应至少返回文本或文件写入结果");
        assertNotNull(savedDir.get(), "源码写入完成后应回调保存目录");
        assertTrue(new File(savedDir.get(), "index.html").isFile());
        assertTrue(new File(savedDir.get(), "package.json").isFile());
        assertTrue(new File(savedDir.get(), "src/App.vue").isFile());
    }
}
