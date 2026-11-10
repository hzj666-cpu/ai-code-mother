package com.hzj.aicodemother.mapper;

import com.hzj.aicodemother.model.entity.AppAsset;
import com.mybatisflex.core.BaseMapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Delete;
import com.hzj.aicodemother.model.dto.app.HistoryAssetLink;
import java.util.List;
import java.time.LocalDateTime;

public interface AppAssetMapper extends BaseMapper<AppAsset> {
    @Insert("INSERT INTO chat_history_asset (chatHistoryId, assetId) VALUES (#{historyId}, #{assetId})")
    int bindHistory(@Param("assetId") Long assetId, @Param("historyId") Long historyId);

    @Select("<script>SELECT chatHistoryId, assetId FROM chat_history_asset WHERE chatHistoryId IN "
            + "<foreach collection='ids' item='id' open='(' separator=',' close=')'>#{id}</foreach></script>")
    List<HistoryAssetLink> listHistoryLinks(@Param("ids") List<Long> ids);

    @Delete("DELETE FROM chat_history_asset WHERE assetId = #{assetId}")
    int deleteHistoryLinksForAsset(@Param("assetId") Long assetId);

    @Delete("DELETE FROM app_asset WHERE id = #{assetId} AND NOT EXISTS "
            + "(SELECT 1 FROM chat_history_asset WHERE assetId = #{assetId})")
    int deleteUnreferenced(@Param("assetId") Long assetId);

    @Select("SELECT a.* FROM app_asset a LEFT JOIN chat_history_asset h ON h.assetId = a.id "
            + "WHERE h.assetId IS NULL AND a.createTime < #{cutoff} ORDER BY a.createTime LIMIT 500")
    List<AppAsset> listExpiredUnreferenced(@Param("cutoff") LocalDateTime cutoff);
}
