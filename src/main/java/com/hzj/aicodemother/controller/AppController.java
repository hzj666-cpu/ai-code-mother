package com.hzj.aicodemother.controller;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
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
import com.hzj.aicodemother.model.dto.app.AppAddRequest;
import com.hzj.aicodemother.model.dto.app.AppAdminUpdateRequest;

import com.hzj.aicodemother.model.dto.app.AppQueryRequest;
import com.hzj.aicodemother.model.dto.app.AppUpdateRequest;
import com.hzj.aicodemother.model.entity.App;
import com.hzj.aicodemother.model.entity.User;
import com.hzj.aicodemother.model.vo.AppVO;
import com.hzj.aicodemother.service.AppService;
import com.hzj.aicodemother.service.UserService;
import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 应用 控制层。
 *
 * @author <a href="https://github.com/">程序员拉丽</a>
 */
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
        App app = new App();
        BeanUtil.copyProperties(appAddRequest, app);
        app.setUserId(loginUser.getId());
        app.setAppName(initPrompt.substring(0, Math.min(initPrompt.length(),12)));
        //暂时改为多文件生成
        app.setCodeGenType(CodeGenTypeEnum.MULTI_FILE.getValue());
        boolean result = appService.save(app);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(app.getId());
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
    public BaseResponse<Boolean> deleteMyApp(@RequestBody DeleteRequest deleteRequest, HttpServletRequest request) {
        if (deleteRequest == null || deleteRequest.getId() <= 0) {
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
    public BaseResponse<Boolean> deleteAppByAdmin(@RequestBody DeleteRequest deleteRequest) {
        if (deleteRequest == null || deleteRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        App oldApp = appService.getById(deleteRequest.getId());
        ThrowUtils.throwIf(oldApp == null, ErrorCode.NOT_FOUND_ERROR);
        boolean result = appService.removeById(deleteRequest.getId());
        return ResultUtils.success(result);
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
