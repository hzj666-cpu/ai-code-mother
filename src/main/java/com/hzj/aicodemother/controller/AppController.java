package com.hzj.aicodemother.controller;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.hzj.aicodemother.ai.AICodeGeneratorServiceFactory;
import com.hzj.aicodemother.ai.model.enums.CodeGenTypeEnum;
import com.hzj.aicodemother.annntation.AuthCheck;
import com.hzj.aicodemother.common.BaseResponse;
import com.hzj.aicodemother.common.DeleteRequest;
import com.hzj.aicodemother.common.ResultUtils;
import com.hzj.aicodemother.constant.AppConstant;
import com.hzj.aicodemother.constant.UserConstant;
import com.hzj.aicodemother.exception.BusinessException;
import com.hzj.aicodemother.exception.ErrorCode;
import com.hzj.aicodemother.exception.ThrowUtils;
import com.hzj.aicodemother.model.dto.app.*;

import com.hzj.aicodemother.model.entity.App;
import com.hzj.aicodemother.model.entity.User;
import com.hzj.aicodemother.model.vo.AppVO;
import com.hzj.aicodemother.manager.CosManager;
import com.hzj.aicodemother.core.builder.VueProjectTemplateService;
import com.hzj.aicodemother.service.AppService;
import com.hzj.aicodemother.service.AppAssetService;
import com.hzj.aicodemother.service.ChatHistoryService;
import com.hzj.aicodemother.service.ProjectDownloadService;
import com.hzj.aicodemother.service.UserService;
import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.http.ResponseEntity;
import org.springframework.http.CacheControl;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.File;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 应用 控制层。
 *
 * @author <a href="https://github.com/">程序员拉丽</a>
 */
@Slf4j
@RestController
@RequestMapping("/app")
public class AppController {

    /**
     * 用户端单页最大条数
     */
    private static final long MAX_PAGE_SIZE = 20;

    @Resource
    private AppService appService;

    @Resource
    private UserService userService;

    @Resource
    private ChatHistoryService chatHistoryService;

    @Resource
    private AICodeGeneratorServiceFactory aiCodeGeneratorServiceFactory;

    @Resource
    private CosManager cosManager;
    @Resource
    private AppAssetService appAssetService;

    @Resource
    private ProjectDownloadService projectDownloadService;
    // region 用户端接口

    /**
     * 创建应用（登录即可，userId 从登录态获取，须填写 initPrompt）
     *
     * @param appAddRequest 创建应用请求
     * @return 新应用 id
     */
    @PostMapping("/add")
    public BaseResponse<Long> addApp(@RequestBody AppAddRequest appAddRequest, HttpServletRequest request) {
        ThrowUtils.throwIf(appAddRequest == null, ErrorCode.PARAMS_ERROR);
        String initPrompt = appAddRequest.getInitPrompt();
        ThrowUtils.throwIf(StrUtil.isBlank(initPrompt), ErrorCode.PARAMS_ERROR, "应用初始化 prompt 不能为空");
        // 获取当前登录用户作为创建者
        User loginUser = userService.getLoginUser(request);
        Long appId = appService.createApp(appAddRequest, loginUser);
        return ResultUtils.success(appId);
    }

    /**
     * 根据 id 修改自己的应用（目前只支持修改应用名称）
     *
     * @param appUpdateRequest 编辑应用请求
     * @return 修改结果
     */
    @PostMapping("/update/my")
    public BaseResponse<Boolean> updateMyApp(@RequestBody AppUpdateRequest appUpdateRequest, HttpServletRequest request) {
        if (appUpdateRequest == null || appUpdateRequest.getId() == null || appUpdateRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        String appName = appUpdateRequest.getAppName();
        if (StrUtil.isBlank(appName) || appName.length() > 64) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "应用名称非法");
        }
        // 判断应用是否存在且属于当前登录用户
        User loginUser = userService.getLoginUser(request);
        App oldApp = appService.getById(appUpdateRequest.getId());
        ThrowUtils.throwIf(oldApp == null, ErrorCode.NOT_FOUND_ERROR);
        if (!oldApp.getUserId().equals(loginUser.getId())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
        // 只修改应用名称
        App app = new App();
        app.setId(appUpdateRequest.getId());
        app.setAppName(appName);
        app.setEditTime(LocalDateTime.now());
        boolean result = appService.updateById(app);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(true);
    }

    /**
     * 根据 id 删除自己的应用
     *
     * @param deleteRequest 删除请求
     * @return 删除结果
     */
    @PostMapping("/delete/my")
    @Transactional
    public BaseResponse<Boolean> deleteMyApp(@RequestBody DeleteRequest deleteRequest, HttpServletRequest request) {
        if (deleteRequest == null || deleteRequest.getId() == null || deleteRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        // 判断应用是否存在且属于当前登录用户
        User loginUser = userService.getLoginUser(request);
        App oldApp = appService.getById(deleteRequest.getId());
        ThrowUtils.throwIf(oldApp == null, ErrorCode.NOT_FOUND_ERROR);
        //仅自己和管理员才可以进行删除
        if (!oldApp.getUserId().equals(loginUser.getId()) && !UserConstant.ADMIN_ROLE.equals(loginUser.getUserRole())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
        boolean result = appService.removeById(deleteRequest.getId());
        ThrowUtils.throwIf(result && !chatHistoryService.removeByAppId(deleteRequest.getId()),
                ErrorCode.OPERATION_ERROR, "删除应用对话历史失败");
        if (result) {
            cleanupAppResourcesAfterCommit(deleteRequest.getId(), oldApp.getCover());
        }
        return ResultUtils.success(result);
    }

    /**
     * 根据 id 获取包装类（应用详情）
     *
     * @param id 应用 id
     * @return 脱敏后的应用详情
     */
    @GetMapping("/get/vo")
    public BaseResponse<AppVO> getAppVOById(long id, HttpServletRequest request) {
        ThrowUtils.throwIf(id <= 0, ErrorCode.PARAMS_ERROR);
        // 需登录才能查看详情
        userService.getLoginUser(request);
        App app = appService.getById(id);
        ThrowUtils.throwIf(app == null, ErrorCode.NOT_FOUND_ERROR);
        return ResultUtils.success(appService.getAppVO(app));
    }

    /**
     * 分页获取当前用户的应用封装列表（支持根据名称查询）
     *
     * @param appQueryRequest 查询请求参数
     * @return 分页结果
     */
    @PostMapping("/my/list/page/vo")
    public BaseResponse<Page<AppVO>> listMyAppVOByPage(@RequestBody AppQueryRequest appQueryRequest,
                                                       HttpServletRequest request) {
        ThrowUtils.throwIf(appQueryRequest == null, ErrorCode.PARAMS_ERROR);
        User loginUser = userService.getLoginUser(request);
        // 强制以登录用户为查询条件，忽略请求中的 userId，防止越权查询他人应用
        appQueryRequest.setUserId(loginUser.getId());
        long pageNum = appQueryRequest.getPageNum();
        long pageSize = appQueryRequest.getPageSize();
        ThrowUtils.throwIf(pageSize > MAX_PAGE_SIZE, ErrorCode.PARAMS_ERROR,
                "单页条数不能超过 " + MAX_PAGE_SIZE);
        QueryWrapper queryWrapper = appService.getQueryWrapper(appQueryRequest);
        Page<App> appPage = appService.page(Page.of(pageNum, pageSize), queryWrapper);
        // 数据脱敏 convertAppVOPage封装了
        return ResultUtils.success(convertAppVOPage(appPage, pageNum, pageSize));
    }

    /**
     * 分页获取精选应用封装列表（priority > 0，按优先级降序，支持根据名称查询）
     *
     * @param appQueryRequest 查询请求参数
     * @return 分页结果
     */
    @PostMapping("/featured/list/page/vo")
    public BaseResponse<Page<AppVO>> listFeaturedAppVOByPage(@RequestBody AppQueryRequest appQueryRequest) {
        ThrowUtils.throwIf(appQueryRequest == null, ErrorCode.PARAMS_ERROR);
        long pageNum = appQueryRequest.getPageNum();
        long pageSize = appQueryRequest.getPageSize();
        ThrowUtils.throwIf(pageSize > MAX_PAGE_SIZE, ErrorCode.PARAMS_ERROR,
                "单页条数不能超过 " + MAX_PAGE_SIZE);
        // 精选 = 优先级大于 0，默认按优先级降序
        appQueryRequest.setPriority(AppConstant.GOOD_APP_PRIORITY);
        QueryWrapper queryWrapper = appService.getQueryWrapper(appQueryRequest);

//        queryWrapper.gt("priority", 0);
//        queryWrapper.orderBy("priority", false);
        Page<App> appPage = appService.page(Page.of(pageNum, pageSize), queryWrapper);
        // 数据脱敏
        return ResultUtils.success(convertAppVOPage(appPage, pageNum, pageSize));
    }

    /**
     * AI 对话生成代码（SSE 流式，仅应用本人可用）
     *
     * 事件协议：
     * - message 事件（默认）：原生模式为原始 token，Vue 模式为带 type 的 JSON 消息
     * - done 事件：流结束标志
     * - error 事件：流中异常，data 为错误信息
     *
     * @param chatToGenCodeRequest 生成请求（appId + 对话提示词）
     * @param request              用于获取登录态
     * @return SSE 事件流
     */
    @PostMapping(value = "/chat/gen/code", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<String>> chatToGenCode(@RequestBody ChatToGenCodeRequest chatToGenCodeRequest,
                                                       HttpServletRequest request) {
        return Flux.defer(() -> {
                    ThrowUtils.throwIf(chatToGenCodeRequest == null
                                    || chatToGenCodeRequest.getAppId() == null || chatToGenCodeRequest.getAppId() <= 0,
                            ErrorCode.PARAMS_ERROR, "应用ID不合法");
                    ThrowUtils.throwIf(StrUtil.isBlank(chatToGenCodeRequest.getChatPrompt()),
                            ErrorCode.PARAMS_ERROR, "对话内容不能为空");
                    // 鉴权（仅本人）与参数校验下沉在 Service，Controller 只取登录态
                    User loginUser = userService.getLoginUser(request);
                    AtomicReference<String> savedDirRef = new AtomicReference<>();
                    return appService
                            .chatToGenCode(chatToGenCodeRequest.getAppId(), chatToGenCodeRequest.getChatPrompt(),
                                    chatToGenCodeRequest.getAssetIds(),
                                    loginUser, dir -> savedDirRef.set(dir.getAbsolutePath()))
                            .map(chunk -> ServerSentEvent.<String>builder().data(chunk).build())
                            .concatWith(Mono.fromSupplier(() ->
                                    ServerSentEvent.<String>builder().event("done")
                                            .data(StrUtil.blankToDefault(savedDirRef.get(), "succeed")).build()));
                })
                .onErrorResume(e -> {
                    Long appId = chatToGenCodeRequest == null ? null : chatToGenCodeRequest.getAppId();
                    boolean hasImages = chatToGenCodeRequest != null
                            && chatToGenCodeRequest.getAssetIds() != null
                            && !chatToGenCodeRequest.getAssetIds().isEmpty();
                    if (hasImages) {
                        log.error("带图对话生成代码失败, appId={}, errorType={}", appId, e.getClass().getSimpleName());
                    } else {
                        log.error("对话生成代码失败, appId={}", appId, e);
                    }
                    return Flux.just(ServerSentEvent.<String>builder().event("error")
                            .data(hasImages ? "图片处理或生成失败，请稍后重试"
                                    : StrUtil.blankToDefault(e.getMessage(), "生成失败，请稍后重试")).build());
                });
    }

    @PostMapping(value = "/{appId}/assets", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public BaseResponse<AppAssetVO> uploadAsset(@PathVariable Long appId,
                                                  @RequestParam("file") MultipartFile file,
                                                  @RequestParam("purpose") String purpose,
                                                  HttpServletRequest request) {
        return ResultUtils.success(appAssetService.upload(appId, userService.getLoginUser(request), file, purpose));
    }

    @GetMapping("/assets/{assetId}/content")
    public ResponseEntity<byte[]> readAsset(@PathVariable Long assetId, HttpServletRequest request) {
        var user = userService.getLoginUser(request);
        var asset = appAssetService.requireOwned(assetId, user);
        return ResponseEntity.ok().contentType(MediaType.parseMediaType(asset.getMimeType()))
                .cacheControl(CacheControl.noStore())
                .header("X-Content-Type-Options", "nosniff")
                .body(appAssetService.read(assetId, user));
    }

    @PostMapping("/assets/{assetId}/delete")
    public BaseResponse<Boolean> deletePendingAsset(@PathVariable Long assetId, HttpServletRequest request) {
        appAssetService.deleteUnreferenced(assetId, userService.getLoginUser(request));
        return ResultUtils.success(true);
    }

    /**
     * 应用部署
     *
     * @param appDeployRequest 部署请求
     * @param request          请求
     * @return 部署 URL
     */
    @PostMapping("/deploy")
    public BaseResponse<String> deployApp(@RequestBody AppDeployRequest appDeployRequest, HttpServletRequest request) {
        ThrowUtils.throwIf(appDeployRequest == null, ErrorCode.PARAMS_ERROR);
        Long appId = appDeployRequest.getAppId();
        ThrowUtils.throwIf(appId == null || appId <= 0, ErrorCode.PARAMS_ERROR, "应用 ID 不能为空");
        // 获取当前登录用户
        User loginUser = userService.getLoginUser(request);
        // 调用服务部署应用
        String deployUrl = appService.deployApp(appId, loginUser);
        return ResultUtils.success(deployUrl);
    }



    /**
     * 下载应用代码
     *
     * @param appId    应用ID
     * @param request  请求
     * @param response 响应
     */
    @GetMapping("/download/{appId}")
    public void downloadAppCode(@PathVariable Long appId,
                                HttpServletRequest request,
                                HttpServletResponse response) {
        // 1. 基础校验
        ThrowUtils.throwIf(appId == null || appId <= 0, ErrorCode.PARAMS_ERROR, "应用ID无效");
        // 2. 查询应用信息
        App app = appService.getById(appId);
        ThrowUtils.throwIf(app == null, ErrorCode.NOT_FOUND_ERROR, "应用不存在");
        // 3. 权限校验：只有应用创建者可以下载代码
        User loginUser = userService.getLoginUser(request);
        if (!app.getUserId().equals(loginUser.getId())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "无权限下载该应用代码");
        }
        // 4. 构建应用代码目录路径（生成目录，非部署目录）
        String codeGenType = app.getCodeGenType();
        String sourceDirName = codeGenType + "_" + appId;
        String sourceDirPath = AppConstant.CODE_OUTPUT_ROOT_DIR + File.separator + sourceDirName;
        // 5. 检查代码目录是否存在
        File sourceDir = new File(sourceDirPath);
        ThrowUtils.throwIf(!sourceDir.exists() || !sourceDir.isDirectory(),
                ErrorCode.NOT_FOUND_ERROR, "应用代码不存在，请先生成代码");
        // 6. 生成下载文件名（不建议添加中文内容）
        String downloadFileName = String.valueOf(appId);
        // 7. 调用通用下载服务
        projectDownloadService.downloadProjectAsZip(sourceDirPath, downloadFileName, response);
    }



    // endregion

    // region 管理员端接口

    /**
     * 根据 id 获取应用（仅管理员）
     *
     * @param id 应用 id
     * @return 应用详情
     */
    @GetMapping("/admin/get")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<App> getAppById(long id) {
        ThrowUtils.throwIf(id <= 0, ErrorCode.PARAMS_ERROR);
        App app = appService.getById(id);
        ThrowUtils.throwIf(app == null, ErrorCode.NOT_FOUND_ERROR);
        return ResultUtils.success(app);
    }

    /**
     * 删除应用（仅管理员，可删除任意应用）
     *
     * @param deleteRequest 删除请求
     * @return 删除结果
     */
    @PostMapping("/admin/delete")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    @Transactional
    public BaseResponse<Boolean> deleteAppByAdmin(@RequestBody DeleteRequest deleteRequest) {
        if (deleteRequest == null || deleteRequest.getId() == null || deleteRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        App oldApp = appService.getById(deleteRequest.getId());
        ThrowUtils.throwIf(oldApp == null, ErrorCode.NOT_FOUND_ERROR);
        boolean result = appService.removeById(deleteRequest.getId());
        ThrowUtils.throwIf(result && !chatHistoryService.removeByAppId(deleteRequest.getId()),
                ErrorCode.OPERATION_ERROR, "删除应用对话历史失败");
        if (result) {
            cleanupAppResourcesAfterCommit(deleteRequest.getId(), oldApp.getCover());
        }
        return ResultUtils.success(result);
    }

    private void cleanupAppResourcesAfterCommit(Long appId, String coverUrl) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    cleanupAppResources(appId, coverUrl);
                }
            });
            return;
        }
        cleanupAppResources(appId, coverUrl);
    }

    private void cleanupAppResources(Long appId, String coverUrl) {
        try {
            appAssetService.cleanupApp(appId);
        } catch (Exception e) {
            log.error("清理应用图片失败, appId={}", appId, e);
        }
        try {
            aiCodeGeneratorServiceFactory.evictAppChatMemory(appId);
        } catch (Exception e) {
            log.error("清理应用对话内存失败, appId={}", appId, e);
        }
        try {
            if (cosManager.deleteGeneratedScreenshot(coverUrl)) {
                log.info("已删除应用自动封面, appId={}", appId);
            }
        } catch (Exception e) {
            log.error("删除应用自动封面失败, appId={}", appId, e);
        }
    }

    /**
     * 更新应用（仅管理员，支持更新应用名称、应用封面、优先级）
     *
     * @param appAdminUpdateRequest 更新应用请求
     * @return 更新结果
     */
    @PostMapping("/admin/update")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> updateAppByAdmin(@RequestBody AppAdminUpdateRequest appAdminUpdateRequest) {
        if (appAdminUpdateRequest == null || appAdminUpdateRequest.getId() == null || appAdminUpdateRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        // 优先级非空时必须非负
//        Integer priority = appAdminUpdateRequest.getPriority();
//        if (priority != null && priority < 0) {
//            throw new BusinessException(ErrorCode.PARAMS_ERROR, "优先级非法");
//        }
        //判断是否存在
        App oldApp =appService.getById(appAdminUpdateRequest.getId());
        ThrowUtils.throwIf(oldApp == null, ErrorCode.NOT_FOUND_ERROR);

        App app = new App();
        BeanUtil.copyProperties(appAdminUpdateRequest, app);
        app.setEditTime(LocalDateTime.now());
        boolean result = appService.updateById(app);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(true);
    }

    /**
     * 分页获取应用封装列表（仅管理员，支持根据除时间外的任何字段查询，每页数量不限）
     *
     * @param appQueryRequest 查询请求参数
     * @return 分页结果
     */
    @PostMapping("/admin/list/page/vo")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Page<AppVO>> listAppVOByPage(@RequestBody AppQueryRequest appQueryRequest) {
        ThrowUtils.throwIf(appQueryRequest == null, ErrorCode.PARAMS_ERROR);
        long pageNum = appQueryRequest.getPageNum();
        long pageSize = appQueryRequest.getPageSize();
        ThrowUtils.throwIf(pageSize <= 0, ErrorCode.PARAMS_ERROR, "单页条数非法");
        Page<App> appPage = appService.page(Page.of(pageNum, pageSize),
                appService.getQueryWrapper(appQueryRequest));
        // 数据脱敏
        return ResultUtils.success(convertAppVOPage(appPage, pageNum, pageSize));
    }

    /**
     * 管理员根据 id 获取应用详情
     *
     * @param id 应用 id
     * @return 应用详情
     */
    @GetMapping("/admin/get/vo")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<AppVO> getAppVOByIdByAdmin(long id) {
        ThrowUtils.throwIf(id <= 0, ErrorCode.PARAMS_ERROR);
        // 查询数据库
        App app = appService.getById(id);
        ThrowUtils.throwIf(app == null, ErrorCode.NOT_FOUND_ERROR);
        // 获取封装类
        return ResultUtils.success(appService.getAppVO(app));
    }


    // endregion

    /**
     * App 分页结果转 AppVO 分页结果（数据脱敏） 封装
     *
     * @param appPage  应用分页结果
     * @param pageNum  当前页号
     * @param pageSize 每页大小
     * @return AppVO 分页结果
     */
    private Page<AppVO> convertAppVOPage(Page<App> appPage, long pageNum, long pageSize) {
        Page<AppVO> appVOPage = new Page<>(pageNum, pageSize, appPage.getTotalRow());
        List<AppVO> appVOList = appService.getAppVOList(appPage.getRecords());
        appVOPage.setRecords(appVOList);
        return appVOPage;
    }

}
