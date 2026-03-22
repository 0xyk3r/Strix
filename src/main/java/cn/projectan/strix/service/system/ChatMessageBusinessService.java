package cn.projectan.strix.service.system;

import cn.hutool.core.util.IdUtil;
import cn.projectan.strix.mapper.system.ChatMessageMapper;
import cn.projectan.strix.mapper.system.ChatSessionMemberMapper;
import cn.projectan.strix.model.constant.system.ChatRedisKeys;
import cn.projectan.strix.model.db.system.ChatMessage;
import cn.projectan.strix.model.db.system.ChatSession;
import cn.projectan.strix.model.db.system.ChatSessionMember;
import cn.projectan.strix.model.db.system.SystemUser;
import cn.projectan.strix.model.enums.system.ChatMessageTypeEnum;
import cn.projectan.strix.model.event.ChatNewMessageEvent;
import cn.projectan.strix.model.request.srv.chat.MarkReadReq;
import cn.projectan.strix.model.request.srv.chat.PullMessageReq;
import cn.projectan.strix.model.request.srv.chat.SendMessageReq;
import cn.projectan.strix.model.response.srv.chat.ChatMessageResp;
import cn.projectan.strix.model.response.srv.chat.SendMessageResultResp;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 聊天消息业务服务
 * <p>
 * 消息收发、已读标记等操作
 *
 * @author ProjectAn
 * @since 2026/2/1 12:00
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ChatMessageBusinessService {

    private final ChatSessionService chatSessionService;
    private final ChatSessionMemberService chatSessionMemberService;
    private final ChatMessageService chatMessageService;
    private final ChatCardDataService chatCardDataService;
    private final ChatSessionMemberMapper chatSessionMemberMapper;
    private final ChatMessageMapper chatMessageMapper;
    private final RedisTemplate<String, Object> redisTemplate;
    private final ApplicationEventPublisher eventPublisher;
    private final SystemUserService systemUserService;

    /**
     * 拉取消息
     *
     * @param req    请求
     * @param userId 当前用户 ID
     * @return 消息列表
     */
    public List<ChatMessageResp> pullMessages(PullMessageReq req, String userId) {
        Assert.isTrue(isSessionMember(req.getSessionId(), userId), "您不是该会话的成员");

        int safeLimit = Math.max(1, Math.min(req.getLimit() != null ? req.getLimit() : 20, 100));

        LambdaQueryWrapper<ChatMessage> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ChatMessage::getSessionId, req.getSessionId());

        List<ChatMessage> messages;

        if (StringUtils.hasText(req.getLastMessageId())) {
            queryWrapper.gt(ChatMessage::getId, req.getLastMessageId())
                    .orderByAsc(ChatMessage::getId)
                    .last("LIMIT " + safeLimit);
            messages = chatMessageMapper.selectList(queryWrapper);
        } else if (StringUtils.hasText(req.getFirstMessageId())) {
            queryWrapper.lt(ChatMessage::getId, req.getFirstMessageId())
                    .orderByDesc(ChatMessage::getId)
                    .last("LIMIT " + safeLimit);
            messages = chatMessageMapper.selectList(queryWrapper);
            Collections.reverse(messages);
        } else {
            queryWrapper.orderByDesc(ChatMessage::getId)
                    .last("LIMIT " + safeLimit);
            messages = chatMessageMapper.selectList(queryWrapper);
            Collections.reverse(messages);
        }

        if (messages.isEmpty()) {
            return new ArrayList<>();
        }

        Set<String> senderIds = messages.stream()
                .map(ChatMessage::getFormUserId)
                .filter(StringUtils::hasText)
                .collect(Collectors.toSet());

        Map<String, SystemUser> senderMap = senderIds.isEmpty()
                ? Collections.emptyMap()
                : systemUserService.listByIds(senderIds).stream()
                .collect(Collectors.toMap(SystemUser::getId, s -> s));

        return messages.stream()
                .map(msg -> buildMessageResp(msg, senderMap))
                .toList();
    }

    /**
     * 发送消息
     *
     * @param req    请求
     * @param userId 当前用户 ID
     * @return 发送结果
     */
    @Transactional(rollbackFor = Exception.class)
    public SendMessageResultResp sendMessage(SendMessageReq req, String userId) {
        Assert.isTrue(isSessionMember(req.getSessionId(), userId), "您不是该会话的成员");

        ChatMessageTypeEnum msgType = ChatMessageTypeEnum.parseFromCodeValue(req.getMsgType());
        Assert.notNull(msgType, "消息类型无效");

        // 幂等检查
        String idempotentKey = ChatRedisKeys.CHAT_MESSAGE_IDEMPOTENT_PREFIX + req.getClientMsgId();
        String existingMsgId = (String) redisTemplate.opsForValue().get(idempotentKey);

        if (StringUtils.hasText(existingMsgId)) {
            ChatMessage existingMsg = chatMessageService.getById(existingMsgId);
            if (existingMsg != null) {
                log.info("消息幂等命中: clientMsgId={}, messageId={}", req.getClientMsgId(), existingMsgId);
                return buildSendResultResp(existingMsg);
            }
        }

        validateMessageContent(req, msgType);

        ChatMessage message = new ChatMessage();
        message.setId(IdUtil.getSnowflakeNextIdStr());
        message.setSessionId(req.getSessionId());
        message.setFormUserId(userId);
        message.setMsgType(req.getMsgType());
        message.setContent(req.getContent());
        message.setImageFileId(req.getImageFileId());
        message.setCardType(req.getCardType());
        message.setCardDataId(req.getCardDataId());
        message.setSendTime(LocalDateTime.now());

        chatMessageService.save(message);

        // 自动取消隐藏
        unhideSessionForReceivers(req.getSessionId(), userId);

        // 更新会话最后消息
        ChatSession session = chatSessionService.getById(req.getSessionId());
        session.setLastMsgId(message.getId());
        session.setLastMsgTime(message.getSendTime());
        session.setUpdatedTime(LocalDateTime.now());
        chatSessionService.updateById(session);

        // 写入幂等 Key
        redisTemplate.opsForValue().set(idempotentKey, message.getId(),
                ChatRedisKeys.CHAT_MESSAGE_IDEMPOTENT_EXPIRE, TimeUnit.SECONDS);

        eventPublisher.publishEvent(new ChatNewMessageEvent(this, req.getSessionId(), message.getId()));

        log.info("消息发送成功: sessionId={}, messageId={}, msgType={}", req.getSessionId(), message.getId(), req.getMsgType());
        return buildSendResultResp(message);
    }

    /**
     * 标记已读
     *
     * @param req    请求
     * @param userId 当前用户 ID
     */
    @Transactional(rollbackFor = Exception.class)
    public void markRead(MarkReadReq req, String userId) {
        Assert.isTrue(isSessionMember(req.getSessionId(), userId), "您不是该会话的成员");

        LambdaQueryWrapper<ChatSessionMember> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ChatSessionMember::getSessionId, req.getSessionId())
                .eq(ChatSessionMember::getUserId, userId);

        ChatSessionMember member = chatSessionMemberMapper.selectOne(queryWrapper);
        if (member != null) {
            member.setLastReadId(req.getLastReadId());
            member.setUpdatedTime(LocalDateTime.now());
            chatSessionMemberService.updateById(member);
            log.info("标记已读成功: sessionId={}, userId={}, lastReadId={}", req.getSessionId(), userId, req.getLastReadId());
        }
    }

    // ========== 包内可见方法（供 ChatBusinessService 使用） ==========

    /**
     * 构建消息响应（含发送者信息）
     */
    ChatMessageResp buildMessageResp(ChatMessage message, Map<String, SystemUser> senderMap) {
        ChatMessageResp resp = new ChatMessageResp(message);

        if (senderMap != null && StringUtils.hasText(message.getFormUserId())) {
            SystemUser sender = senderMap.get(message.getFormUserId());
            if (sender != null) {
                resp.setFromUserName(sender.getNickname());
            }
        }

        if (ChatMessageTypeEnum.CARD.getCodeValue().equals(message.getMsgType())
                && StringUtils.hasText(message.getCardType())
                && StringUtils.hasText(message.getCardDataId())) {
            Object cardData = chatCardDataService.getCardData(message.getCardType(), message.getCardDataId());
            resp.setCardData(cardData);
        }

        return resp;
    }

    /**
     * 生成消息摘要
     */
    String generateMessagePreview(ChatMessage message) {
        if (message == null) {
            return "";
        }

        ChatMessageTypeEnum msgType = ChatMessageTypeEnum.parseFromCodeValue(message.getMsgType());
        if (msgType == null) {
            return "";
        }

        return switch (msgType) {
            case TEXT -> {
                if (StringUtils.hasText(message.getContent())) {
                    yield message.getContent().length() > 50
                            ? message.getContent().substring(0, 50) + "..."
                            : message.getContent();
                }
                yield "";
            }
            case IMAGE -> "[图片]";
            case CARD -> "[卡片]";
        };
    }

    // ========== 私有方法 ==========

    private boolean isSessionMember(String sessionId, String userId) {
        LambdaQueryWrapper<ChatSessionMember> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ChatSessionMember::getSessionId, sessionId)
                .eq(ChatSessionMember::getUserId, userId);
        return chatSessionMemberMapper.exists(queryWrapper);
    }

    private void unhideSessionForReceivers(String sessionId, String senderId) {
        boolean updated = chatSessionMemberService.lambdaUpdate()
                .eq(ChatSessionMember::getSessionId, sessionId)
                .ne(ChatSessionMember::getUserId, senderId)
                .eq(ChatSessionMember::getHiddenStatus, 1)
                .set(ChatSessionMember::getHiddenStatus, (short) 0)
                .set(ChatSessionMember::getUpdatedTime, LocalDateTime.now())
                .update();
        if (updated) {
            log.info("自动取消会话隐藏: sessionId={}", sessionId);
        }
    }

    private void validateMessageContent(SendMessageReq req, ChatMessageTypeEnum msgType) {
        switch (msgType) {
            case TEXT:
                Assert.hasText(req.getContent(), "文本消息内容不能为空");
                break;
            case IMAGE:
                Assert.hasText(req.getImageFileId(), "图片文件 ID 不能为空");
                break;
            case CARD:
                Assert.hasText(req.getCardType(), "卡片类型不能为空");
                Assert.hasText(req.getCardDataId(), "卡片数据 ID 不能为空");
                Assert.isTrue(chatCardDataService.isSupportedCardType(req.getCardType()), "不支持的卡片类型");
                break;
        }
    }

    private SendMessageResultResp buildSendResultResp(ChatMessage message) {
        SendMessageResultResp resp = new SendMessageResultResp();
        resp.setMessageId(message.getId());
        resp.setSessionId(message.getSessionId());
        resp.setSendTime(message.getSendTime());
        return resp;
    }

}
