package com.hzj.aicodemother.service;

import com.hzj.aicodemother.model.dto.chathistory.ChatHistoryQueryRequest;
import com.hzj.aicodemother.model.entity.ChatHistory;
import com.hzj.aicodemother.model.vo.ChatHistoryVO;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.core.service.IService;

import java.util.List;

/**
 * 聊天历史服务接口，继承自IService<ChatHistory>
 * 提供聊天历史的增删改查等操作
 */
public interface ChatHistoryService extends IService<ChatHistory> {

    /**
     * 根据应用ID删除聊天历史记录
     * @param appId 应用ID
     * @return 删除是否成功
     */
    boolean removeByAppId(Long appId);

    /**
     * Query records with a composite createTime/id cursor.
     *
     * @param queryRequest query conditions and cursor
     * @param limit        maximum records to return
     * @return records ordered from newest to oldest
     */
    List<ChatHistory> listByCursor(ChatHistoryQueryRequest queryRequest, long limit);

    /**
     * 将ChatHistory对象转换为ChatHistoryVO视图对象
     * @param chatHistory 聊天历史实体对象
     * @return 聊天历史视图对象
     */
    ChatHistoryVO getChatHistoryVO(ChatHistory chatHistory);

    /**
     * 将ChatHistory对象列表转换为ChatHistoryVO视图对象列表
     * @param chatHistoryList 聊天历史实体对象列表
     * @return 聊天历史视图对象列表
     */
    List<ChatHistoryVO> getChatHistoryVOList(List<ChatHistory> chatHistoryList);

    /**
     * 根据查询请求参数构建QueryWrapper对象
     * @param chatHistoryQueryRequest 聊天历史查询请求对象
     * @QueryWrapper 用于构建数据库查询条件的包装器
     */
    QueryWrapper getQueryWrapper(ChatHistoryQueryRequest chatHistoryQueryRequest);
}
