package com.hzj.aicodemother.controller;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.hzj.aicodemother.common.BaseResponse;
import com.hzj.aicodemother.common.DeleteRequest;
import com.hzj.aicodemother.common.ResultUtils;
import com.hzj.aicodemother.constant.UserConstant;
import com.hzj.aicodemother.annntation.AuthCheck;
import com.hzj.aicodemother.exception.BusinessException;
import com.hzj.aicodemother.exception.ErrorCode;
import com.hzj.aicodemother.exception.ThrowUtils;
import com.hzj.aicodemother.model.dto.chathistory.ChatHistoryAddRequest;
import com.hzj.aicodemother.model.dto.chathistory.ChatHistoryQueryRequest;
import com.hzj.aicodemother.model.dto.chathistory.ChatHistoryUpdateRequest;
import com.hzj.aicodemother.model.entity.App;
import com.hzj.aicodemother.model.entity.ChatHistory;
import com.hzj.aicodemother.model.entity.User;
import com.hzj.aicodemother.model.enums.ChatHistoryMessageTypeEnum;
import com.hzj.aicodemother.model.vo.ChatHistoryCursorPageVO;
import com.hzj.aicodemother.model.vo.ChatHistoryVO;
import com.hzj.aicodemother.service.AppService;
import com.hzj.aicodemother.service.ChatHistoryService;
import com.hzj.aicodemother.service.UserService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Chat history controller.
 */
@RestController
@RequestMapping("/chatHistory")
public class ChatHistoryController {

    private static final long MAX_PAGE_SIZE = 50;

    private static final long USER_PAGE_SIZE = 10;

    @Resource
    private ChatHistoryService chatHistoryService;

    @Resource
    private UserService userService;

    @Resource
    private AppService appService;

    /**
     * Create a chat history record for the current user.
     */
    @PostMapping("/add")
    public BaseResponse<Long> addChatHistory(@RequestBody ChatHistoryAddRequest addRequest,
                                             HttpServletRequest request) {
        validateMessageRequest(addRequest);
        User loginUser = userService.getLoginUser(request);
        checkAppPermission(addRequest.getAppId(), loginUser);

        ChatHistory chatHistory = new ChatHistory();
        BeanUtil.copyProperties(addRequest, chatHistory);
        chatHistory.setUserId(loginUser.getId());
        boolean result = chatHistoryService.save(chatHistory);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(chatHistory.getId());
    }

    /**
     * Get one chat history record owned by the current user.
     */
    @GetMapping("/get")
    public BaseResponse<ChatHistoryVO> getChatHistoryById(long id, HttpServletRequest request) {
        ThrowUtils.throwIf(id <= 0, ErrorCode.PARAMS_ERROR);
        User loginUser = userService.getLoginUser(request);
        ChatHistory chatHistory = chatHistoryService.getById(id);
        ThrowUtils.throwIf(chatHistory == null, ErrorCode.NOT_FOUND_ERROR);
        checkChatHistoryPermission(chatHistory, loginUser);
        return ResultUtils.success(chatHistoryService.getChatHistoryVO(chatHistory));
    }

    /**
     * Update the message content or message type of an owned record.
     */
    @PostMapping("/update")
    public BaseResponse<Boolean> updateChatHistory(@RequestBody ChatHistoryUpdateRequest updateRequest,
                                                   HttpServletRequest request) {
        if (updateRequest == null || updateRequest.getId() == null || updateRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        validateMessage(updateRequest.getMessage(), updateRequest.getMessageType());
        User loginUser = userService.getLoginUser(request);
        ChatHistory oldChatHistory = chatHistoryService.getById(updateRequest.getId());
        ThrowUtils.throwIf(oldChatHistory == null, ErrorCode.NOT_FOUND_ERROR);
        checkChatHistoryPermission(oldChatHistory, loginUser);

        ChatHistory chatHistory = new ChatHistory();
        BeanUtil.copyProperties(updateRequest, chatHistory);
        boolean result = chatHistoryService.updateById(chatHistory);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(true);
    }

    /**
     * Logically delete an owned chat history record.
     */
    @PostMapping("/delete")
    public BaseResponse<Boolean> deleteChatHistory(@RequestBody DeleteRequest deleteRequest,
                                                   HttpServletRequest request) {
        if (deleteRequest == null || deleteRequest.getId() == null || deleteRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = userService.getLoginUser(request);
        ChatHistory chatHistory = chatHistoryService.getById(deleteRequest.getId());
        ThrowUtils.throwIf(chatHistory == null, ErrorCode.NOT_FOUND_ERROR);
        checkChatHistoryPermission(chatHistory, loginUser);
        return ResultUtils.success(chatHistoryService.removeById(deleteRequest.getId()));
    }

    /**
     * 浏览当前用户的聊天记录，可以选择按应用筛选。
     */
    @PostMapping("/list/page/vo")
    public BaseResponse<ChatHistoryCursorPageVO> listChatHistoryVOByPage(
            @RequestBody ChatHistoryQueryRequest queryRequest, HttpServletRequest request) {
        ThrowUtils.throwIf(queryRequest == null, ErrorCode.PARAMS_ERROR);
        User loginUser = userService.getLoginUser(request);
        ThrowUtils.throwIf(queryRequest.getAppId() == null || queryRequest.getAppId() <= 0,
                ErrorCode.PARAMS_ERROR, "应用 ID 不能为空");
        checkAppPermission(queryRequest.getAppId(), loginUser);
        if (!UserConstant.ADMIN_ROLE.equals(loginUser.getUserRole())) {
            queryRequest.setUserId(loginUser.getId());
        }

        long pageSize = queryRequest.getPageSize();
        ThrowUtils.throwIf(pageSize <= 0 || pageSize > USER_PAGE_SIZE,
                ErrorCode.PARAMS_ERROR, "单页消息数量不能超过 " + USER_PAGE_SIZE);
        queryRequest.setSortField("createTime");
        queryRequest.setSortOrder("descend");
        return ResultUtils.success(queryCursorPage(queryRequest, pageSize));
    }

    /**
     * Page through all applications' chat history for administrators.
     */
    @PostMapping("/admin/list/page/vo")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<ChatHistoryCursorPageVO> listAllChatHistoryVOByPage(
            @RequestBody ChatHistoryQueryRequest queryRequest) {
        ThrowUtils.throwIf(queryRequest == null, ErrorCode.PARAMS_ERROR);
        long pageSize = queryRequest.getPageSize();
        ThrowUtils.throwIf(pageSize <= 0 || pageSize > MAX_PAGE_SIZE,
                ErrorCode.PARAMS_ERROR, "分页参数不合法，单页数量不能超过 " + MAX_PAGE_SIZE);
        queryRequest.setUserId(null);
        queryRequest.setSortField("createTime");
        queryRequest.setSortOrder("descend");
        return ResultUtils.success(queryCursorPage(queryRequest, pageSize));
    }

    private ChatHistoryCursorPageVO queryCursorPage(ChatHistoryQueryRequest queryRequest, long pageSize) {
        List<ChatHistory> chatHistoryList = chatHistoryService.listByCursor(queryRequest, pageSize + 1);
        boolean hasMore = chatHistoryList.size() > pageSize;
        if (hasMore) {
            chatHistoryList = chatHistoryList.subList(0, (int) pageSize);
        }

        List<ChatHistoryVO> records = chatHistoryService.getChatHistoryVOList(chatHistoryList);
        ChatHistoryCursorPageVO result = new ChatHistoryCursorPageVO();
        result.setRecords(records);
        result.setPageSize(pageSize);
        result.setHasMore(hasMore);
        if (!chatHistoryList.isEmpty()) {
            ChatHistory lastRecord = chatHistoryList.get(chatHistoryList.size() - 1);
            result.setNextBeforeCreateTime(lastRecord.getCreateTime());
            result.setNextBeforeId(lastRecord.getId());
        }
        return result;
    }

    private void validateMessageRequest(ChatHistoryAddRequest addRequest) {
        if (addRequest == null || addRequest.getAppId() == null || addRequest.getAppId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        validateMessage(addRequest.getMessage(), addRequest.getMessageType());
    }

    private void validateMessage(String message, String messageType) {
        if (StrUtil.isBlank(message) || ChatHistoryMessageTypeEnum.getEnumByValue(messageType) == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "消息内容或消息类型不合法");
        }
    }

    private void checkAppPermission(Long appId, User loginUser) {
        App app = appService.getById(appId);
        ThrowUtils.throwIf(app == null, ErrorCode.NOT_FOUND_ERROR, "应用不存在");
        if (!UserConstant.ADMIN_ROLE.equals(loginUser.getUserRole())
                && !loginUser.getId().equals(app.getUserId())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "无权访问该应用的对话历史");
        }
    }

    private void checkChatHistoryPermission(ChatHistory chatHistory, User loginUser) {
        checkAppPermission(chatHistory.getAppId(), loginUser);
    }
}
