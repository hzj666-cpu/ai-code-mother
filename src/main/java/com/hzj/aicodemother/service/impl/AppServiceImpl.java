package com.hzj.aicodemother.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.hzj.aicodemother.ai.model.enums.CodeGenTypeEnum;
import com.hzj.aicodemother.ai.model.message.StreamMessageTypeEnum;
import com.hzj.aicodemother.constant.AppConstant;
import com.hzj.aicodemother.core.AiCodeGeneratorFacade;
import com.hzj.aicodemother.core.builder.VueProjectBuilder;
import com.hzj.aicodemother.exception.BusinessException;
import com.hzj.aicodemother.exception.ErrorCode;
import com.hzj.aicodemother.exception.ThrowUtils;
import com.hzj.aicodemother.mapper.UserMapper;
import com.hzj.aicodemother.manager.CosManager;
import com.hzj.aicodemother.model.dto.app.AppQueryRequest;
import com.hzj.aicodemother.model.entity.App;
import com.hzj.aicodemother.model.entity.ChatHistory;
import com.hzj.aicodemother.model.entity.User;
import com.hzj.aicodemother.model.enums.ChatHistoryMessageTypeEnum;
import com.hzj.aicodemother.model.vo.AppVO;
import com.hzj.aicodemother.model.vo.UserVO;
import com.hzj.aicodemother.service.AppService;
import com.hzj.aicodemother.service.ChatHistoryService;
import com.hzj.aicodemother.service.ScreenshotService;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import com.hzj.aicodemother.mapper.AppMapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.io.File;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;
import java.util.function.Function;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * 应用 服务层实现。
 *
 * @author <a href="https://github.com/">程序员拉丽</a>
 */
@Service
@Slf4j
public class AppServiceImpl extends ServiceImpl<AppMapper, App> implements AppService {

    @Resource
    private UserMapper userMapper;
    @Resource
    private AiCodeGeneratorFacade aiCodeGeneratorFacade;

    @Resource
    private ChatHistoryService chatHistoryService;

    @Resource
    private ObjectMapper objectMapper;
    @Resource
    private VueProjectBuilder vueProjectBuilder;
    @Resource
    private ScreenshotService screenshotService;
    @Resource
    private CosManager cosManager;

    @Resource(name = "screenshotTaskExecutor")
    private Executor screenshotTaskExecutor;

    @Override
    public Flux<String> chatToGenCode(Long appId, String message, User loginUser, Consumer<File> onSaved) {
        //参数校验
        ThrowUtils.throwIf(appId == null, ErrorCode.PARAMS_ERROR, "应用ID不能为空");
        ThrowUtils.throwIf(StringUtils.isBlank(message), ErrorCode.PARAMS_ERROR, "用户消息不能为空");
        //查询应用信息
        App app = this.getById(appId);
        ThrowUtils.throwIf(app == null, ErrorCode.PARAMS_ERROR, "应用不存在");
        //验证用户是否有权限访问该应用，仅本人可以生成代码
        if (!app.getUserId().equals(loginUser.getId())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "无权限访问该应用");
        }
        //获取应用的代码生成类型
        String codeGenTypeStr = app.getCodeGenType();
        ThrowUtils.throwIf(StringUtils.isBlank(codeGenTypeStr), ErrorCode.PARAMS_ERROR, "该应用未设置生成类型");
        CodeGenTypeEnum codeGenTypeEnum = CodeGenTypeEnum.getEnumByValue(codeGenTypeStr);
        ThrowUtils.throwIf(codeGenTypeEnum == null, ErrorCode.PARAMS_ERROR, "生成类型不合法");
        // Hydrate Redis memory from existing database history before persisting this turn.
        aiCodeGeneratorFacade.initializeAppChatMemory(appId);
        saveChatHistory(appId, loginUser.getId(), message, ChatHistoryMessageTypeEnum.USER);
        //调用AI生成代码（onSaved 落盘回调由调用方决定用途：Controller 用于 done 事件、部署流程用于取目录；不需要时传 null）
        Flux<String> aiStream;
        try {
            aiStream = aiCodeGeneratorFacade.generateAndSaveCodeStream(message, codeGenTypeEnum, onSaved, appId);
        } catch (RuntimeException e) {
            saveErrorChatHistory(appId, loginUser.getId(), e);
            throw e;
        }
        return Flux.defer(() -> {
            StringBuilder aiMessageBuffer = new StringBuilder();
            // toolId -> 已解析的 arguments；request 时暂存，executed 时合并落库（靠 id 关联）
            Map<String, JsonNode> pendingToolRequests = new HashMap<>();
            return aiStream
                .doOnNext(chunk -> {
                    if (codeGenTypeEnum != CodeGenTypeEnum.VUE_PROJECT) {
                        aiMessageBuffer.append(chunk);
                        return;
                    }
                    handleVueStreamChunk(chunk, aiMessageBuffer, pendingToolRequests, appId, loginUser.getId());
                })
                .doOnComplete(() -> saveChatHistory(appId, loginUser.getId(), aiMessageBuffer.toString(),
                        ChatHistoryMessageTypeEnum.AI))
                .doOnError(error -> saveErrorChatHistory(appId, loginUser.getId(), error));
        });
    }

    /** Accumulate AI response text for the turn, and persist each tool call as a
     * structured `tool` history record when it finishes (content merged from the
     * stashed request arguments, correlated by tool id). */
    void handleVueStreamChunk(String chunk, StringBuilder history, Map<String, JsonNode> pendingToolRequests,
                              Long appId, Long userId) {
        try {
            JsonNode event = objectMapper.readTree(chunk);
            String type = event.path("type").asText();
            if (StreamMessageTypeEnum.AI_RESPONSE.getValue().equals(type)) {
                history.append(event.path("data").asText());
            } else if (StreamMessageTypeEnum.TOOL_REQUEST.getValue().equals(type)) {
                String toolId = event.path("id").asText();
                if (!toolId.isBlank()) {
                    pendingToolRequests.put(toolId, parseToolArguments(event.path("arguments").asText()));
                }
            } else if (StreamMessageTypeEnum.TOOL_EXECUTED.getValue().equals(type)) {
                String toolId = event.path("id").asText();
                JsonNode arguments = toolId.isBlank() ? null : pendingToolRequests.remove(toolId);
                saveToolChatHistory(event, arguments, appId, userId);
            }
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("无效的 Vue 流式消息", e);
        }
    }

    /** Model-generated arguments may be malformed JSON; degrade to null instead of failing the stream. */
    private JsonNode parseToolArguments(String arguments) {
        if (StrUtil.isBlank(arguments)) {
            return null;
        }
        try {
            return objectMapper.readTree(arguments);
        } catch (JsonProcessingException e) {
            log.warn("工具调用参数解析失败，降级为无参数记录", e);
            return null;
        }
    }

    /** message 列为 text(64KB 字节)；按字符数截断到 20000，中文 3 字节/字也必然安全 */
    private static final int TOOL_CONTENT_MAX_CHARS = 20_000;

    private void saveToolChatHistory(JsonNode executedEvent, JsonNode arguments, Long appId, Long userId) {
        try {
            ObjectNode payload = objectMapper.createObjectNode();
            payload.put("name", executedEvent.path("name").asText());
            if (arguments != null && arguments.isObject()) {
                payload.put("filePath", arguments.path("relativeFilePath").asText(""));
                String content = arguments.path("content").asText("");
                if (content.length() > TOOL_CONTENT_MAX_CHARS) {
                    content = content.substring(0, TOOL_CONTENT_MAX_CHARS) + "\n…(内容过长，已截断)";
                }
                payload.put("content", content);
            }
            payload.put("result", executedEvent.path("result").asText(""));
            payload.put("failed", executedEvent.path("failed").asBoolean(false));
            ChatHistory chatHistory = ChatHistory.builder()
                    .appId(appId)
                    .userId(userId)
                    .message(objectMapper.writeValueAsString(payload))
                    .messageType(ChatHistoryMessageTypeEnum.TOOL.getValue())
                    .build();
            // 历史持久化失败不应中断代码生成主流程，只记日志
            if (!chatHistoryService.save(chatHistory)) {
                log.error("保存工具调用历史失败, appId={}", appId);
            }
        } catch (RuntimeException | JsonProcessingException e) {
            log.error("保存工具调用历史失败, appId={}", appId, e);
        }
    }

    private void saveChatHistory(Long appId, Long userId, String message,
                                 ChatHistoryMessageTypeEnum messageType) {
        ChatHistory chatHistory = ChatHistory.builder()
                .appId(appId)
                .userId(userId)
                .message(message)
                .messageType(messageType.getValue())
                .build();
        boolean result = chatHistoryService.save(chatHistory);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR, "保存对话历史失败");
    }

    private void saveErrorChatHistory(Long appId, Long userId, Throwable error) {
        try {
            saveChatHistory(appId, userId,
                    "AI generation failed: " + StrUtil.blankToDefault(error.getMessage(), "unknown error"),
                    ChatHistoryMessageTypeEnum.AI);
        } catch (RuntimeException historyException) {
            log.error("保存 AI 错误对话历史失败, appId={}, userId={}", appId, userId, historyException);
        }
    }
    @Override
    public AppVO getAppVO(App app) {
        if (app == null) {
            return null;
        }
        AppVO appVO = new AppVO();
        BeanUtil.copyProperties(app, appVO);
        // 填充创建用户脱敏信息
        Long userId = app.getUserId();
        if (userId != null) {
            User user = userMapper.selectOneById(userId);
            if (user != null) {
                UserVO userVO = new UserVO();
                BeanUtil.copyProperties(user, userVO);
                appVO.setUser(userVO);
            }
        }
        return appVO;
    }

    @Override
    public List<AppVO> getAppVOList(List<App> appList) {
        if (CollUtil.isEmpty(appList)) {
            return new ArrayList<>();
        }
        // 批量查询创建用户，避免循环内逐条查库
        Set<Long> userIdSet = appList.stream()
                .map(App::getUserId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, User> userMap = CollUtil.isEmpty(userIdSet) ? Map.of()
                : userMapper.selectListByIds(userIdSet).stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));
        return appList.stream().map(app -> {
            AppVO appVO = new AppVO();
            BeanUtil.copyProperties(app, appVO);
            User user = userMap.get(app.getUserId());
            if (user != null) {
                UserVO userVO = new UserVO();
                BeanUtil.copyProperties(user, userVO);
                appVO.setUser(userVO);
            }
            return appVO;
        }).collect(Collectors.toList());
    }

    @Override
    public QueryWrapper getQueryWrapper(AppQueryRequest appQueryRequest) {
        if (appQueryRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请求参数为空");
        }
        Long id = appQueryRequest.getId();
        String appName = appQueryRequest.getAppName();
        String cover = appQueryRequest.getCover();
        String initPrompt = appQueryRequest.getInitPrompt();
        String codeGenType = appQueryRequest.getCodeGenType();
        String deployKey = appQueryRequest.getDeployKey();
        Integer priority = appQueryRequest.getPriority();
        Long userId = appQueryRequest.getUserId();
        String sortField = appQueryRequest.getSortField();
        String sortOrder = appQueryRequest.getSortOrder();
        return QueryWrapper.create()
                .eq("id", id)
                .eq("codeGenType", codeGenType)
                .eq("deployKey", deployKey)
                .eq("priority", priority)
                .eq("userId", userId)
                .like("appName", appName)
                .like("cover", cover)
                .like("initPrompt", initPrompt)
                .orderBy(sortField, "ascend".equals(sortOrder));
    }
    @Override
    public String deployApp(Long appId, User loginUser) {
        // 1. 参数校验
        ThrowUtils.throwIf(appId == null || appId <= 0, ErrorCode.PARAMS_ERROR, "应用 ID 不能为空");
        ThrowUtils.throwIf(loginUser == null, ErrorCode.NOT_LOGIN_ERROR, "用户未登录");
        // 2. 查询应用信息
        App app = this.getById(appId);
        ThrowUtils.throwIf(app == null, ErrorCode.NOT_FOUND_ERROR, "应用不存在");
        // 3. 验证用户是否有权限部署该应用，仅本人可以部署
        if (!app.getUserId().equals(loginUser.getId())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "无权限部署该应用");
        }
        // 4. 检查是否已有 deployKey
        String deployKey = app.getDeployKey();
        // 没有则生成 6 位 deployKey（大小写字母 + 数字）
        if (StrUtil.isBlank(deployKey)) {
            deployKey = RandomUtil.randomString(6);
        }
        // 5. 获取代码生成类型，构建源目录路径
        String codeGenType = app.getCodeGenType();
        String sourceDirName = codeGenType + "_" + appId;
        String sourceDirPath = AppConstant.CODE_OUTPUT_ROOT_DIR + File.separator + sourceDirName;
        // 6. 检查源目录是否存在
        File sourceDir = new File(sourceDirPath);
        if (!sourceDir.exists() || !sourceDir.isDirectory()) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "应用代码不存在，请先生成代码");
        }
       // 7. Vue 项目特殊处理：执行构建
        CodeGenTypeEnum codeGenTypeEnum = CodeGenTypeEnum.getEnumByValue(codeGenType);
        if (codeGenTypeEnum == CodeGenTypeEnum.VUE_PROJECT) {
            // Vue 项目需要构建
            boolean buildSuccess = vueProjectBuilder.buildProject(sourceDirPath);
            ThrowUtils.throwIf(!buildSuccess, ErrorCode.SYSTEM_ERROR, "Vue 项目构建失败，请检查代码和依赖");
            // 检查 dist 目录是否存在
            File distDir = new File(sourceDirPath, "dist");
            ThrowUtils.throwIf(!distDir.exists(), ErrorCode.SYSTEM_ERROR, "Vue 项目构建完成但未生成 dist 目录");
            // 将 dist 目录作为部署源
            sourceDir = distDir;
            log.info("Vue 项目构建成功，将部署 dist 目录: {}", distDir.getAbsolutePath());
        }
// 8. 复制文件到部署目录
        String deployDirPath = AppConstant.CODE_DEPLOY_ROOT_DIR + File.separator + deployKey;
        try {
            FileUtil.copyContent(sourceDir, new File(deployDirPath), true);
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "部署失败：" + e.getMessage());
        }
        // 9. 更新应用的 deployKey 和部署时间
        App updateApp = new App();
        updateApp.setId(appId);
        updateApp.setDeployKey(deployKey);
        updateApp.setDeployedTime(LocalDateTime.now());
        boolean updateResult = this.updateById(updateApp);
        ThrowUtils.throwIf(!updateResult, ErrorCode.OPERATION_ERROR, "更新应用部署信息失败");
        // 10. 构建应用访问 URL
        String appDeployUrl = String.format("%s/%s/", AppConstant.CODE_DEPLOY_HOST, deployKey);
       // 11. 异步生成截图并更新应用封面
        generateAppScreenshotAsync(appId, appDeployUrl);
        return appDeployUrl;

    }

    @Override
    public void generateAppScreenshotAsync(Long appId, String appUrl) {
        try {
            screenshotTaskExecutor.execute(() -> {
                try {
                    String screenshotUrl = screenshotService.generateAndUploadScreenshot(appUrl);
                    App updateApp = new App();
                    updateApp.setId(appId);
                    updateApp.setCover(screenshotUrl);
                    if (!this.updateById(updateApp)) {
                        log.error("更新应用封面失败, appId={}", appId);
                        try {
                            cosManager.deleteGeneratedScreenshot(screenshotUrl);
                        } catch (Exception cleanupError) {
                            log.warn("清理未关联的应用封面失败, appId={}", appId, cleanupError);
                        }
                    }
                } catch (Exception e) {
                    log.error("应用封面截图任务失败, appId={}, url={}", appId, appUrl, e);
                }
            });
        } catch (RejectedExecutionException e) {
            log.warn("应用封面截图任务队列已满, appId={}, url={}", appId, appUrl, e);
        }
    }
}
