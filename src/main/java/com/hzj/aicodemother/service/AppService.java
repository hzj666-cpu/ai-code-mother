package com.hzj.aicodemother.service;

import com.hzj.aicodemother.model.dto.app.AppQueryRequest;
import com.hzj.aicodemother.model.vo.AppVO;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.core.service.IService;
import com.hzj.aicodemother.model.entity.App;

import java.util.List;

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

}
