package com.hzj.aicodemother.ai;

import com.hzj.aicodemother.ai.model.HtmlCodeResult;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class AICodeGeneratorServiceFactoryTest {
    @Resource
    private AICodeGeneratorServiceFactory aICodeGeneratorServiceFactory;

    @Test
    void generatorHtmlCode() {
        AICodeGeneratorService aiCodeGeneratorService = aICodeGeneratorServiceFactory.createAICodeGeneratorService();
        String s = aiCodeGeneratorService.generatorHtmlCode("做一个程序员拉丽的计算器小工具");
        assertNotNull(s);
    }
    @Test
    void generatorMultiCode() {
        AICodeGeneratorService aiCodeGeneratorService = aICodeGeneratorServiceFactory.createAICodeGeneratorService();
        String s = aiCodeGeneratorService.generatorMultiFileCode("做一个程序员拉丽的日历表");
        assertNotNull(s);
    }

    @Test
    void generateHtmlCodeResult(){
        AICodeGeneratorService aiCodeGeneratorService = aICodeGeneratorServiceFactory.createAICodeGeneratorService();
        HtmlCodeResult htmlCodeResult = aiCodeGeneratorService.generateHtmlCodeResult("做一个程序员拉丽的简单计算器小工具");
        assertNotNull(htmlCodeResult);

    }
}