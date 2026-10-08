package com.hzj.aicodemother.core.saver;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import com.hzj.aicodemother.ai.model.enums.CodeGenTypeEnum;
import com.hzj.aicodemother.constant.AppConstant;
import com.hzj.aicodemother.exception.BusinessException;
import com.hzj.aicodemother.exception.ErrorCode;

import java.io.File;
import java.nio.charset.StandardCharsets;

/**
 * 代码文件保存模板基类（模板方法模式）
 *
 * 定义通用保存流程：类型校验 -> 建唯一目录 -> 子类写文件。
 * 子类只需声明支持的生成类型、结果类型和写文件细节
 * （如多文件模式写 3 个文件，HTML 单文件模式只写 1 个）。
 *
 * @param <T> 该模式对应的结果类型（如 HtmlCodeResult / MultiFileCodeResult）
 */
public abstract class AbstractCodeFileSaverTemplate<T> {

    /**
     * 文件保存根目录
     */
   // private static final String FILE_SAVE_ROOT_DIR = System.getProperty("user.dir") + "/tmp/code_output";
    // 文件保存根目录
    protected static final String FILE_SAVE_ROOT_DIR = AppConstant.CODE_OUTPUT_ROOT_DIR;

    /**
     * 保存流程模板（final 锁定流程骨架）
     * @param appId 应用ID
     * @param result 解析后的结果对象（由执行器按生成类型分发传入）
     * @return 保存目录
     */
    public final File save(Object result, Long appId) {
        //验证输入
        T typedResult = castResult(result);
        //构建唯一目录
        String baseDirPath = buildUniqueDir(appId);
        //保存文件
        writeFiles(baseDirPath, typedResult);
        //返回目录
        return new File(baseDirPath);
    }

    /**
     * 本模板支持的生成类型
     */
    protected abstract CodeGenTypeEnum getBizType();

    /**
     * 本模板接受的结果类型（用于运行时类型校验）
     */
    protected abstract Class<T> resultType();

    /**
     * 钩子方法：子类实现具体写文件逻辑
     *
     * @param dirPath 保存目录
     * @param result  强类型结果对象
     */
    protected abstract void writeFiles(String dirPath, T result);

    /**
     * 受控类型转换：全项目唯一的"Object -> 具体结果类型"桥接点，
     * 类型不匹配时抛业务异常而非 ClassCastException
     */
    private T castResult(Object result) {
        if (result == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "保存内容为空");
        }
        if (!resultType().isInstance(result)) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR,
                    StrUtil.format("保存内容类型不匹配：期望 {}，实际 {}",
                            resultType().getSimpleName(), result.getClass().getSimpleName()));
        }
        return resultType().cast(result);
    }

    /**
     * 构建唯一目录路径：tmp/code_output/bizType_雪花ID
     * 基于appId创建目录路径
     */
    private String buildUniqueDir(Long appId) {
        if (appId == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "appId不能为空");
        }
        String bizType = getBizType().getValue();
        String uniqueDirName = StrUtil.format("{}_{}", bizType, appId);
        String dirPath = FILE_SAVE_ROOT_DIR + File.separator + uniqueDirName;
        FileUtil.mkdir(dirPath);
        return dirPath;
    }

    /**
     * 写入单个文件
     */
    protected void writeToFile(String dirPath, String filename, String content) {
        String filePath = dirPath + File.separator + filename;
        FileUtil.writeString(content, filePath, StandardCharsets.UTF_8);
    }
}
