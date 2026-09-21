package com.hzj.aicodemother.service;

import com.hzj.aicodemother.model.dto.app.AppQueryRequest;
import com.hzj.aicodemother.model.entity.User;
import com.hzj.aicodemother.model.vo.AppVO;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.core.service.IService;
import com.hzj.aicodemother.model.entity.App;
import reactor.core.publisher.Flux;

import java.io.File;
import java.util.List;
import java.util.function.Consumer;

/**
 * 应用 服务层。
 *
 * @author <a href="https://github.com/">程序员拉丽</a>
 */
public interface AppService extends IService<App> {

    /**
     * 获取单个脱敏应用信息
     *
     * @param app 应用实体对象
     * @return AppVO 应用视图对象
     */
    AppVO getAppVO(App app);

    /**
     * 获取脱敏应用信息列表
     *
     * @param appList 应用实体列表
     * @return AppVO 列表
     */
    List<AppVO> getAppVOList(List<App> appList);

    /**
     * 获取查询条件
     *
     * @param appQueryRequest 查询请求参数
     * @return QueryWrapper 查询条件
     */
    QueryWrapper getQueryWrapper(AppQueryRequest appQueryRequest);


    /**
     * AI 生成代码（SSE 流式，仅应用本人可用）
     *
     * @param appId     应用ID（决定生成类型与保存目录）
     * @param message   用户提示词
     * @param loginUser 当前登录用户
     * @param onSaved   落盘完成回调，参数为保存目录（供调用方追加 done 事件 / 触发部署），可为 null
     * @return 原始 token 分片流（前端打字机直接渲染）
     */
    Flux<String> chatToGenCode(Long appId, String message, User loginUser, Consumer<File> onSaved);
}
