package com.hzj.aicodemother.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import com.hzj.aicodemother.exception.BusinessException;
import com.hzj.aicodemother.exception.ErrorCode;
import com.hzj.aicodemother.mapper.UserMapper;
import com.hzj.aicodemother.model.dto.app.AppQueryRequest;
import com.hzj.aicodemother.model.entity.App;
import com.hzj.aicodemother.model.entity.User;
import com.hzj.aicodemother.model.vo.AppVO;
import com.hzj.aicodemother.model.vo.UserVO;
import com.hzj.aicodemother.service.AppService;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import com.hzj.aicodemother.mapper.AppMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 应用 服务层实现。
 *
 * @author <a href="https://github.com/">程序员拉丽</a>
 */
@Service
public class AppServiceImpl extends ServiceImpl<AppMapper, App> implements AppService {

    @Resource
    private UserMapper userMapper;

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

}
