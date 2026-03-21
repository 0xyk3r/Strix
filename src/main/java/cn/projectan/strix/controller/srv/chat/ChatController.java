package cn.projectan.strix.controller.srv.chat;

import cn.projectan.strix.controller.srv.base.BaseSrvController;
import cn.projectan.strix.core.ret.RetBuilder;
import cn.projectan.strix.core.ret.RetResult;
import cn.projectan.strix.model.annotation.StrixLog;
import cn.projectan.strix.model.dict.system.SystemLogOperType;
import cn.projectan.strix.model.request.srv.chat.*;
import cn.projectan.strix.model.response.srv.chat.ChatMessageResp;
import cn.projectan.strix.model.response.srv.chat.ChatSessionListItemResp;
import cn.projectan.strix.model.response.srv.chat.ChatSessionResp;
import cn.projectan.strix.model.response.srv.chat.SendMessageResultResp;
import cn.projectan.strix.service.system.ChatBusinessService;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 聊天控制器
 *
 * @author ProjectAn
 * @since 2026/2/1 12:00
 */
@Slf4j
@RestController
@RequestMapping("/srv/chat")
@Tag(name = "聊天服务", description = "用户端聊天功能接口")
@RequiredArgsConstructor
public class ChatController extends BaseSrvController {

    private final ChatBusinessService chatBusinessService;

    /**
     * 获取或创建会话
     */
    @PostMapping("/session/getOrCreate")
    @StrixLog(operationGroup = "聊天", operationName = "获取或创建会话")
    @Operation(summary = "获取或创建会话", description = "单聊会话会尝试复用，群聊会话每次创建新的")
    public RetResult<ChatSessionResp> getOrCreateSession(@Valid @RequestBody CreateSessionReq req) {
        String userId = getLoginUserId();
        ChatSessionResp resp = chatBusinessService.getOrCreateSession(req, userId);
        return RetBuilder.success(resp);
    }

    /**
     * 会话列表
     */
    @PostMapping("/session/list")
    @StrixLog(operationGroup = "聊天", operationName = "查询会话列表")
    @Operation(summary = "会话列表", description = "分页查询当前用户的会话列表，包含未读数")
    public RetResult<Page<ChatSessionListItemResp>> getSessionList(@Valid @RequestBody SessionListReq req) {
        String userId = getLoginUserId();
        Page<ChatSessionListItemResp> page = chatBusinessService.getSessionList(req, userId);
        return RetBuilder.success(page);
    }

    /**
     * 拉取消息
     */
    @PostMapping("/message/pull")
    @StrixLog(operationGroup = "聊天", operationName = "拉取消息")
    @Operation(summary = "拉取消息", description = "拉取新消息（lastMessageId）或历史消息（firstMessageId）")
    public RetResult<List<ChatMessageResp>> pullMessages(@Valid @RequestBody PullMessageReq req) {
        String userId = getLoginUserId();
        List<ChatMessageResp> messages = chatBusinessService.pullMessages(req, userId);
        return RetBuilder.success(messages);
    }

    /**
     * 发送消息
     */
    @PostMapping("/message/send")
    @StrixLog(operationGroup = "聊天", operationName = "发送消息", operationType = SystemLogOperType.ADD)
    @Operation(summary = "发送消息", description = "发送文本/图片/卡片消息，支持幂等")
    public RetResult<SendMessageResultResp> sendMessage(@Valid @RequestBody SendMessageReq req) {
        String userId = getLoginUserId();
        SendMessageResultResp resp = chatBusinessService.sendMessage(req, userId);
        return RetBuilder.success(resp);
    }

    /**
     * 标记已读
     */
    @PostMapping("/message/markRead")
    @StrixLog(operationGroup = "聊天", operationName = "标记已读", operationType = SystemLogOperType.UPDATE)
    @Operation(summary = "标记已读", description = "标记会话中最后已读消息 ID")
    public RetResult<Void> markRead(@Valid @RequestBody MarkReadReq req) {
        String userId = getLoginUserId();
        chatBusinessService.markRead(req, userId);
        return RetBuilder.success();
    }

    /**
     * 删除或退出会话
     */
    @PostMapping("/session/delete")
    @StrixLog(operationGroup = "聊天", operationName = "删除或退出会话", operationType = SystemLogOperType.DELETE)
    @Operation(summary = "删除或退出会话", description = "一对一会话：隐藏会话（对方发消息时重新显示）；群聊会话：退出会话（OWNER 自动移交）")
    public RetResult<Void> deleteOrLeaveSession(@Valid @RequestBody DeleteSessionReq req) {
        String userId = getLoginUserId();
        chatBusinessService.deleteOrLeaveSession(req, userId);
        return RetBuilder.success();
    }

}
