package com.hzj.aicodemother.core.stream;

import com.hzj.aicodemother.ai.model.enums.CodeGenTypeEnum;
import reactor.core.publisher.Flux;

import java.io.File;
import java.util.Set;
import java.util.function.Consumer;

/** A generation mode owns its stream conversion and completion behavior. */
public interface StreamHandler {

/**
 * 获取支持的代码生成类型集合
 * 该方法用于返回当前代码生成器支持的所有类型枚举
 *
 * @return 返回一个包含支持类型的Set集合，集合中的元素为CodeGenTypeEnum枚举类型
 */
    Set<CodeGenTypeEnum> supportedTypes();

/**
 * 处理用户消息并生成代码的响应流
 *
 * @param userMessage 用户输入的消息内容
 * @param type 代码生成的类型枚举
 * @param onSaved 代码生成完成后的回调函数，用于处理生成的文件
 * @param appId 应用程序ID，用于标识当前请求的应用
 * @return 返回一个包含生成代码的字符串类型的Flux流，响应式编程中的数据流
 */
    Flux<String> handle(String userMessage, CodeGenTypeEnum type, Consumer<File> onSaved, Long appId);
}
