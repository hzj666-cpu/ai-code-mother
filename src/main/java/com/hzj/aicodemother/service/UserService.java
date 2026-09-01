package com.hzj.aicodemother.service;

import com.hzj.aicodemother.model.dto.user.UserQueryRequest;
import com.hzj.aicodemother.model.vo.LoginUserVO;
import com.hzj.aicodemother.model.vo.UserVO;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.core.service.IService;
import com.hzj.aicodemother.model.entity.User;
import jakarta.servlet.http.HttpServletRequest;

import java.net.http.HttpRequest;
import java.util.List;

/**
 * 用户 服务层。
 *
 * @author <a href="https://github.com/">程序员拉丽</a>
 */
public interface UserService extends IService<User> {


    /**
     * 用户注册
     *
     * @param userAccount   用户账户
     * @param userPassword  用户密码
     * @param checkPassword 校验密码
     * @return 新用户 id
     */
    long userRegister(String userAccount, String userPassword, String checkPassword);

    String getEncryptPassword(String userPassword);

    /**
     * 根据用户信息获取登录用户的脱敏信息
     *
     * @param user 用户实体对象，包含用户的基本信息
     * @return LoginUserVO 登录用户视图对象，用于前端展示的用户信息
     */
    LoginUserVO getLoginUserVO(User user);

    //用户登录
    LoginUserVO userLogin(String userAccount, String userPassword, HttpServletRequest request);

    //获取当前登录用户
    User getLoginUser(HttpServletRequest request);

    /**
     * 用户注销
     *
     * @param request
     * @return
     */
    boolean userLogout(HttpServletRequest request);
    //获取单个脱敏用户信息
    UserVO getUserVO(User user);

    List<UserVO> getUserVOList(List<User> userList);

    QueryWrapper getQueryWrapper(UserQueryRequest userQueryRequest);

}
