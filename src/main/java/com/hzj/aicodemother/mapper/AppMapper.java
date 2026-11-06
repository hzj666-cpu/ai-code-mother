package com.hzj.aicodemother.mapper;

import com.mybatisflex.core.BaseMapper;
import com.hzj.aicodemother.model.entity.App;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

/**
 * 应用 映射层。
 *
 * @author <a href="https://github.com/">程序员拉丽</a>
 */
public interface AppMapper extends BaseMapper<App> {

    @Update("UPDATE app SET buildStatus = #{status}, buildError = #{error}, buildToken = #{token} "
            + "WHERE id = #{appId} AND isDelete = 0")
    int beginBuild(@Param("appId") Long appId, @Param("token") String token,
                   @Param("status") String status, @Param("error") String error);

    @Update("UPDATE app SET buildStatus = #{status}, buildError = #{error} "
            + "WHERE id = #{appId} AND buildToken = #{token} "
            + "AND buildStatus = #{expectedStatus} AND isDelete = 0")
    int updateBuildStatus(@Param("appId") Long appId, @Param("token") String token,
                          @Param("expectedStatus") String expectedStatus,
                          @Param("status") String status, @Param("error") String error);

}
