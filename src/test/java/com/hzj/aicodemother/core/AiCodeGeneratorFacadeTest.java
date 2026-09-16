package com.hzj.aicodemother.core;

import com.hzj.aicodemother.ai.model.enums.CodeGenTypeEnum;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.File;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;
@SpringBootTest
class AiCodeGeneratorFacadeTest {
   @Resource
   private AiCodeGeneratorFacade aiCodeGeneratorFacade;
    @Test
    void generateAndSaveCode() {
        File file = aiCodeGeneratorFacade.generateAndSaveCode("做一个程序员拉丽的简单的计算器小工具", CodeGenTypeEnum.MULTI_FILE);

        Assertions.assertNotNull(file);
    }

    @Test
    void generateAndSaveCodeStream() {
        AtomicReference<File> savedDir = new AtomicReference<>();
        List<String> chunks = aiCodeGeneratorFacade
                .generateAndSaveCodeStream("做一个程序员拉丽的简单的计算器小工具", CodeGenTypeEnum.HTML, savedDir::set)
                .collectList()
                .block(Duration.ofMinutes(3));

        Assertions.assertNotNull(chunks);
        Assertions.assertFalse(chunks.isEmpty(), "流式分片不应为空");
        Assertions.assertNotNull(savedDir.get(), "流结束后应触发落盘回调");
        Assertions.assertTrue(new File(savedDir.get(), "index.html").exists(), "index.html 应已保存");
    }
}