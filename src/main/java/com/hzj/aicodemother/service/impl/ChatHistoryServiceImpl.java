package com.hzj.aicodemother.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import com.hzj.aicodemother.exception.BusinessException;
import com.hzj.aicodemother.exception.ErrorCode;
import com.hzj.aicodemother.mapper.ChatHistoryMapper;
import com.hzj.aicodemother.model.dto.chathistory.ChatHistoryQueryRequest;
import com.hzj.aicodemother.model.entity.ChatHistory;
import com.hzj.aicodemother.model.enums.ChatHistoryMessageTypeEnum;
import com.hzj.aicodemother.model.vo.ChatHistoryVO;
import com.hzj.aicodemother.service.ChatHistoryService;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.time.LocalDateTime;
import java.util.function.Consumer;

@Service
public class ChatHistoryServiceImpl extends ServiceImpl<ChatHistoryMapper, ChatHistory>
        implements ChatHistoryService {

    @Override
    public boolean removeByAppId(Long appId) {
        if (appId == null || appId <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "应用 ID 不合法");
        }
        return remove(QueryWrapper.create().eq("appId", appId));
    }

    @Override
    public List<ChatHistory> listByCursor(ChatHistoryQueryRequest queryRequest, long limit) {
        if (limit <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "查询数量必须大于 0");
        }
        QueryWrapper queryWrapper = getQueryWrapper(queryRequest);
        queryWrapper.limit(limit);
        return list(queryWrapper);
    }

    @Override
    public List<ChatHistory> listLatestForMemory(Long appId, int limit) {
        if (appId == null || appId <= 0 || limit <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "应用 ID 或记忆加载数量不合法");
        }
        // 仅取 user/ai 两类：tool 记录无法映射为 ChatMessage，且会挤占记忆窗口名额
        List<ChatHistory> records = list(QueryWrapper.create()
                .eq("appId", appId)
                .in("messageType", ChatHistoryMessageTypeEnum.USER.getValue(),
                        ChatHistoryMessageTypeEnum.AI.getValue())
                .orderBy("createTime", false)
                .orderBy("id", false)
                .limit(limit));
        Collections.reverse(records);
        return records;
    }

/**
 * 将ChatHistory对象转换为ChatHistoryVO对象
 * 这是一个重写的方法，用于实现对象属性的复制和转换
 * @param chatHistory 需要转换的聊天历史记录对象
 * @return 转换后的ChatHistoryVO对象，如果输入为null则返回null
 */
    @Override
    public ChatHistoryVO getChatHistoryVO(ChatHistory chatHistory) {
    // 检查输入参数是否为null，如果是则直接返回null
        if (chatHistory == null) {
            return null;
        }
    // 创建新的ChatHistoryVO对象
        ChatHistoryVO chatHistoryVO = new ChatHistoryVO();
    // 使用BeanUtil工具类将chatHistory的属性复制到chatHistoryVO中
        BeanUtil.copyProperties(chatHistory, chatHistoryVO);
    // 返回转换后的VO对象
        return chatHistoryVO;
    }

    @Override
    public List<ChatHistoryVO> getChatHistoryVOList(List<ChatHistory> chatHistoryList) {
        if (CollUtil.isEmpty(chatHistoryList)) {
            return new ArrayList<>();
        }
        return chatHistoryList.stream()
                .map(this::getChatHistoryVO)
                .collect(Collectors.toList());
    }

    @Override
    /**
     * 根据聊天历史查询请求参数构建查询条件包装器
     * @param chatHistoryQueryRequest 聊天历史查询请求对象，包含查询条件
     * @return QueryWrapper 构建好的查询条件包装器，用于数据库查询
     * @throws BusinessException 当请求参数为空时抛出业务异常
     */
    public QueryWrapper getQueryWrapper(ChatHistoryQueryRequest chatHistoryQueryRequest) {
        // 检查请求参数是否为空，为空则抛出业务异常
        if (chatHistoryQueryRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请求参数为空");
        }
        // 从请求对象中提取查询参数
        Long id = chatHistoryQueryRequest.getId();
        String message = chatHistoryQueryRequest.getMessage();
        String messageType = chatHistoryQueryRequest.getMessageType();
        Long appId = chatHistoryQueryRequest.getAppId();
        Long userId = chatHistoryQueryRequest.getUserId();
        LocalDateTime beforeCreateTime = chatHistoryQueryRequest.getBeforeCreateTime();
        Long beforeId = chatHistoryQueryRequest.getBeforeId();
        String sortField = chatHistoryQueryRequest.getSortField();
        String sortOrder = chatHistoryQueryRequest.getSortOrder();
        // 设置默认排序字段为创建时间
        if (sortField == null || sortField.isBlank()) {
            sortField = "createTime";
        }
        // 创建基础查询条件
        QueryWrapper queryWrapper = QueryWrapper.create()
                .eq("id", id)                    // 精确匹配ID
                .eq("appId", appId)              // 精确匹配应用ID
                .eq("userId", userId)            // 精确匹配用户ID
                .eq("messageType", messageType)  // 精确匹配消息类型
                .like("message", message);      // 模糊匹配消息内容
        // 处理创建时间查询条件
        if (beforeCreateTime != null) {
            if (beforeId == null) {
                // 如果没有指定ID，只查询创建时间早于指定时间的数据
                queryWrapper.lt("createTime", beforeCreateTime);
            } else {
                // 如果指定了ID，查询创建时间早于指定时间或创建时间相同但ID小于指定ID的数据
                queryWrapper.and((Consumer<QueryWrapper>) wrapper -> wrapper
                        .lt("createTime", beforeCreateTime)
                        .or((Consumer<QueryWrapper>) orWrapper -> orWrapper
                                .eq("createTime", beforeCreateTime)
                                .lt("id", beforeId)));
            }
        }
        // 设置排序方式
        boolean ascending = "ascend".equals(sortOrder);
        // 返回设置好排序条件的查询包装器
        return queryWrapper
                .orderBy(sortField, ascending)   // 按指定字段排序
                .orderBy("id", ascending);       // 按ID排序
    }
}
