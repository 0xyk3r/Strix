package cn.projectan.strix.service.system;

import cn.hutool.core.util.IdUtil;
import cn.projectan.strix.mapper.system.ChatMessageMapper;
import cn.projectan.strix.mapper.system.ChatSessionMemberMapper;
import cn.projectan.strix.model.constant.system.ChatRedisKeys;
import cn.projectan.strix.model.db.system.*;
import cn.projectan.strix.model.enums.system.ChatMemberRoleEnum;
import cn.projectan.strix.model.enums.system.ChatMessageTypeEnum;
import cn.projectan.strix.model.enums.system.ChatSessionTypeEnum;
import cn.projectan.strix.model.event.ChatNewMessageEvent;
import cn.projectan.strix.model.event.ChatSessionCreatedEvent;
import cn.projectan.strix.model.request.srv.chat.*;
import cn.projectan.strix.model.response.srv.chat.ChatMessageResp;
import cn.projectan.strix.model.response.srv.chat.ChatSessionListItemResp;
import cn.projectan.strix.model.response.srv.chat.ChatSessionResp;
import cn.projectan.strix.model.response.srv.chat.SendMessageResultResp;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
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
 * 聊天业务服务
 * <p>
 * 核心业务逻辑：会话管理、消息收发、未读数计算等
 *
 * @author ProjectAn
 * @since 2026/2/1 12:00
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ChatBusinessService {

    private final ChatConfigService chatConfigService;
    private final ChatSessionService chatSessionService;
    private final ChatSessionMemberService chatSessionMemberService;
    private final ChatMessageService chatMessageService;
    private final ChatNotificationService chatNotificationService;
    private final ChatCardDataService chatCardDataService;
    private final ChatSessionMemberMapper chatSessionMemberMapper;
    private final ChatMessageMapper chatMessageMapper;
    private final RedisTemplate<String, Object> redisTemplate;
    private final ApplicationEventPublisher eventPublisher;
    private final SystemUserService systemUserService;
    private final UserOnlineStatusService userOnlineStatusService;

    /**
     * 获取或创建会话
     * <p>
     * 单聊场景下，会尝试复用现有会话（相同 config + bizType + bizId + 成员组合）
     * 群聊场景下，每次都创建新会话
     *
     * @param req    请求
     * @param userId 当前用户 ID
     * @return 会话信息
     */
    @Transactional(rollbackFor = Exception.class)
    public ChatSessionResp getOrCreateSession(CreateSessionReq req, String userId) {
        // 1. 查询配置
        ChatConfig config = chatConfigService.lambdaQuery()
                .eq(ChatConfig::getKey, req.getConfigKey())
                .one();
        Assert.notNull(config, "聊天配置不存在");

        ChatSessionTypeEnum sessionType = ChatSessionTypeEnum.parseFromCodeValue(config.getSessionType());
        Assert.notNull(sessionType, "聊天配置类型无效");

        // 2. 单聊场景：尝试复用现有会话
        if (sessionType == ChatSessionTypeEnum.SINGLE) {
            Assert.hasText(req.getOtherUserId(), "单聊场景下对方用户 ID 不能为空");

            ChatSession existingSession = findExistingSingleSession(config.getId(), req.getBizType(), req.getBizId(), userId, req.getOtherUserId());
            if (existingSession != null) {
                log.info("复用现有单聊会话: sessionId={}", existingSession.getId());
                return buildSessionResp(existingSession, config);
            }
        }

        // 3. 群聊场景：验证参数
        if (sessionType == ChatSessionTypeEnum.GROUP) {
            Assert.notNull(req.getMemberUserIds(), "群聊场景下成员列表不能为空");
            Assert.isTrue(!req.getMemberUserIds().isEmpty(), "群聊成员至少需要 1 人");
        }

        // 4. 创建新会话
        ChatSession newSession = new ChatSession();
        newSession.setId(IdUtil.getSnowflakeNextIdStr());
        newSession.setConfigId(config.getId());
        newSession.setType(config.getSessionType());
        newSession.setBizType(req.getBizType());
        newSession.setBizId(req.getBizId());

        // 群聊场景：设置群聊名称
        if (sessionType == ChatSessionTypeEnum.GROUP && StringUtils.hasText(req.getGroupName())) {
            newSession.setGroupName(req.getGroupName());
        }

        chatSessionService.save(newSession);

        // 5. 创建会话成员
        createSessionMembers(newSession.getId(), userId, req, sessionType);

        // 6. 发布会话创建事件（事务提交后会触发 WebSocket 通知）
        eventPublisher.publishEvent(new ChatSessionCreatedEvent(this, newSession.getId()));

        log.info("创建新会话: sessionId={}, type={}, configKey={}", newSession.getId(), sessionType.getCodeValue(), req.getConfigKey());
        return buildSessionResp(newSession, config);
    }

    /**
     * 获取会话列表
     *
     * @param req    请求
     * @param userId 当前用户 ID
     * @return 会话列表
     */
    public Page<ChatSessionListItemResp> getSessionList(SessionListReq req, String userId) {
        // 1. 查询用户参与的会话 ID 列表（排除已隐藏的会话）
        LambdaQueryWrapper<ChatSessionMember> memberQuery = new LambdaQueryWrapper<>();
        memberQuery.eq(ChatSessionMember::getUserId, userId)
                .and(w -> w.isNull(ChatSessionMember::getHiddenStatus).or().eq(ChatSessionMember::getHiddenStatus, 0))
                .orderByDesc(ChatSessionMember::getUpdatedTime);

        List<ChatSessionMember> members = chatSessionMemberMapper.selectList(memberQuery);
        if (members.isEmpty()) {
            return new Page<>(req.getPageIndex(), req.getPageSize());
        }

        List<String> sessionIds = members.stream()
                .map(ChatSessionMember::getSessionId)
                .toList();

        // 2. 分页查询会话
        LambdaQueryWrapper<ChatSession> sessionQuery = new LambdaQueryWrapper<>();
        sessionQuery.in(ChatSession::getId, sessionIds);

        if (StringUtils.hasText(req.getConfigKey())) {
            // 先查询配置 ID
            ChatConfig config = chatConfigService.lambdaQuery()
                    .eq(ChatConfig::getKey, req.getConfigKey())
                    .one();
            if (config != null) {
                sessionQuery.eq(ChatSession::getConfigId, config.getId());
            }
        }

        if (StringUtils.hasText(req.getBizType())) {
            sessionQuery.eq(ChatSession::getBizType, req.getBizType());
        }

        sessionQuery.orderByDesc(ChatSession::getLastMsgTime, ChatSession::getCreatedTime);

        Page<ChatSession> sessionPage = chatSessionService.page(new Page<>(req.getPageIndex(), req.getPageSize()), sessionQuery);

        // 3. 如果没有会话，直接返回空页
        if (sessionPage.getRecords().isEmpty()) {
            return new Page<>(req.getPageIndex(), req.getPageSize(), 0);
        }

        // 4. 批量查询所有会话的成员（用于获取一对一会话的对方用户 ID）
        List<String> pageSessionIds = sessionPage.getRecords().stream()
                .map(ChatSession::getId)
                .toList();

        LambdaQueryWrapper<ChatSessionMember> allMembersQuery = new LambdaQueryWrapper<>();
        allMembersQuery.in(ChatSessionMember::getSessionId, pageSessionIds);
        List<ChatSessionMember> allMembers = chatSessionMemberMapper.selectList(allMembersQuery);

        // 按会话 ID 分组
        Map<String, List<ChatSessionMember>> sessionMembersMap = allMembers.stream()
                .collect(Collectors.groupingBy(ChatSessionMember::getSessionId));

        // 5. 收集所有需要查询的用户 ID
        Set<String> userIdsToQuery = new HashSet<>();
        for (ChatSession session : sessionPage.getRecords()) {
            // 如果是一对一会话，获取对方用户 ID
            if (ChatSessionTypeEnum.SINGLE.getCodeValue().equals(session.getType())) {
                List<ChatSessionMember> sessionMembers = sessionMembersMap.get(session.getId());
                if (sessionMembers != null) {
                    for (ChatSessionMember member : sessionMembers) {
                        if (!member.getUserId().equals(userId)) {
                            userIdsToQuery.add(member.getUserId());
                        }
                    }
                }
            }
            // 收集最后消息的发送者 ID
            if (StringUtils.hasText(session.getLastMsgId())) {
                ChatMessage lastMsg = chatMessageService.getById(session.getLastMsgId());
                if (lastMsg != null) {
                    userIdsToQuery.add(lastMsg.getFormUserId());
                }
            }
        }

        // 6. 批量查询用户信息
        Map<String, SystemUser> userMap = new HashMap<>();
        if (!userIdsToQuery.isEmpty()) {
            List<SystemUser> users = systemUserService.listByIds(userIdsToQuery);
            userMap = users.stream()
                    .collect(Collectors.toMap(SystemUser::getId, u -> u));
        }

        // 7. 批量查询在线状态（仅一对一会话的对方）
        List<String> onlineStatusUserIds = new ArrayList<>();
        for (ChatSession session : sessionPage.getRecords()) {
            if (ChatSessionTypeEnum.SINGLE.getCodeValue().equals(session.getType())) {
                List<ChatSessionMember> sessionMembers = sessionMembersMap.get(session.getId());
                if (sessionMembers != null) {
                    for (ChatSessionMember member : sessionMembers) {
                        if (!member.getUserId().equals(userId)) {
                            onlineStatusUserIds.add(member.getUserId());
                        }
                    }
                }
            }
        }
        Map<String, Boolean> onlineStatusMap = userOnlineStatusService.batchGetOnlineStatus(onlineStatusUserIds);

        // 8. 批量查询最后消息
        List<String> lastMsgIds = sessionPage.getRecords().stream()
                .map(ChatSession::getLastMsgId)
                .filter(StringUtils::hasText)
                .toList();

        Map<String, ChatMessage> lastMessageMap = new HashMap<>();
        if (!lastMsgIds.isEmpty()) {
            List<ChatMessage> lastMessages = chatMessageService.listByIds(lastMsgIds);
            lastMessageMap = lastMessages.stream()
                    .collect(Collectors.toMap(ChatMessage::getId, m -> m));
        }

        // 9. 构建响应
        Page<ChatSessionListItemResp> respPage = new Page<>(req.getPageIndex(), req.getPageSize(), sessionPage.getTotal());
        List<ChatSessionListItemResp> respList = new ArrayList<>();

        Map<String, ChatSessionMember> memberMap = members.stream()
                .collect(Collectors.toMap(ChatSessionMember::getSessionId, m -> m));

        for (ChatSession session : sessionPage.getRecords()) {
            ChatSessionListItemResp resp = new ChatSessionListItemResp();
            ChatConfig config = chatConfigService.getById(session.getConfigId());

            resp.setSessionId(session.getId());
            resp.setConfigId(session.getConfigId());
            resp.setConfigKey(config != null ? config.getKey() : null);
            resp.setConfigName(config != null ? config.getName() : null);
            resp.setSessionType(session.getType());
            resp.setBizType(session.getBizType());
            resp.setBizId(session.getBizId());
            resp.setLastMsgId(session.getLastMsgId());
            resp.setLastMsgTime(session.getLastMsgTime());
            resp.setCreatedTime(session.getCreatedTime());
            resp.setGroupName(session.getGroupName());

            // 设置会话显示名称
            if (ChatSessionTypeEnum.SINGLE.getCodeValue().equals(session.getType())) {
                // 一对一会话：显示对方昵称
                List<ChatSessionMember> sessionMembers = sessionMembersMap.get(session.getId());
                if (sessionMembers != null) {
                    for (ChatSessionMember member : sessionMembers) {
                        if (!member.getUserId().equals(userId)) {
                            SystemUser otherUser = userMap.get(member.getUserId());
                            if (otherUser != null) {
                                resp.setSessionDisplayName(otherUser.getNickname());
                                // 设置对方在线状态
                                resp.setOtherUserOnlineStatus(onlineStatusMap.getOrDefault(member.getUserId(), false));
                            }
                            break;
                        }
                    }
                }
            } else {
                // 群聊会话：显示群聊名称
                resp.setSessionDisplayName(session.getGroupName());
            }

            // 设置最后消息预览和发送者名称
            if (StringUtils.hasText(session.getLastMsgId())) {
                ChatMessage lastMsg = lastMessageMap.get(session.getLastMsgId());
                if (lastMsg != null) {
                    // 生成消息摘要
                    resp.setLastMessagePreview(generateMessagePreview(lastMsg));

                    // 设置发送者名称
                    SystemUser sender = userMap.get(lastMsg.getFormUserId());
                    if (sender != null) {
                        resp.setLastMessageSenderName(sender.getNickname());
                    }
                }
            }

            // 计算未读数
            ChatSessionMember member = memberMap.get(session.getId());
            if (member != null && StringUtils.hasText(session.getLastMsgId())) {
                resp.setUnreadCount(calculateUnreadCount(session.getId(), member.getLastReadId(), userId));
            } else {
                resp.setUnreadCount(0);
            }

            respList.add(resp);
        }

        respPage.setRecords(respList);
        return respPage;
    }

    /**
     * 拉取消息
     *
     * @param req    请求
     * @param userId 当前用户 ID
     * @return 消息列表
     */
    public List<ChatMessageResp> pullMessages(PullMessageReq req, String userId) {
        // 验证用户是会话成员
        Assert.isTrue(isSessionMember(req.getSessionId(), userId), "您不是该会话的成员");

        LambdaQueryWrapper<ChatMessage> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ChatMessage::getSessionId, req.getSessionId());

        List<ChatMessage> messages;

        // 拉取新消息（id > lastMessageId）
        if (StringUtils.hasText(req.getLastMessageId())) {
            queryWrapper.gt(ChatMessage::getId, req.getLastMessageId())
                    .orderByAsc(ChatMessage::getId)
                    .last("LIMIT " + req.getLimit());

            messages = chatMessageMapper.selectList(queryWrapper);
        }
        // 拉取历史消息（id < firstMessageId）
        else if (StringUtils.hasText(req.getFirstMessageId())) {
            queryWrapper.lt(ChatMessage::getId, req.getFirstMessageId())
                    .orderByDesc(ChatMessage::getId)
                    .last("LIMIT " + req.getLimit());

            messages = chatMessageMapper.selectList(queryWrapper);
            // 反转顺序（从旧到新）
            Collections.reverse(messages);
        }
        // 默认拉取最新消息
        else {
            queryWrapper.orderByDesc(ChatMessage::getId)
                    .last("LIMIT " + req.getLimit());

            messages = chatMessageMapper.selectList(queryWrapper);
            Collections.reverse(messages);
        }

        // 如果没有消息，直接返回
        if (messages.isEmpty()) {
            return new ArrayList<>();
        }

        // 批量查询发送者用户信息（性能优化）
        Set<String> senderIds = messages.stream()
                .map(ChatMessage::getFormUserId)
                .filter(StringUtils::hasText)
                .collect(Collectors.toSet());

        Map<String, SystemUser> senderMap = new HashMap<>();
        if (!senderIds.isEmpty()) {
            List<SystemUser> senders = systemUserService.listByIds(senderIds);
            senderMap = senders.stream()
                    .collect(Collectors.toMap(SystemUser::getId, s -> s));
        }

        // 构建响应（填充发送者信息）
        final Map<String, SystemUser> finalSenderMap = senderMap;
        return messages.stream()
                .map(msg -> buildMessageResp(msg, finalSenderMap))
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
        // 1. 验证用户是会话成员
        Assert.isTrue(isSessionMember(req.getSessionId(), userId), "您不是该会话的成员");

        // 2. 验证消息类型
        ChatMessageTypeEnum msgType = ChatMessageTypeEnum.parseFromCodeValue(req.getMsgType());
        Assert.notNull(msgType, "消息类型无效");

        // 3. 幂等检查
        String idempotentKey = ChatRedisKeys.CHAT_MESSAGE_IDEMPOTENT_PREFIX + req.getClientMsgId();
        String existingMsgId = (String) redisTemplate.opsForValue().get(idempotentKey);

        if (StringUtils.hasText(existingMsgId)) {
            // 幂等：返回已有消息
            ChatMessage existingMsg = chatMessageService.getById(existingMsgId);
            if (existingMsg != null) {
                log.info("消息幂等命中: clientMsgId={}, messageId={}", req.getClientMsgId(), existingMsgId);
                return buildSendResultResp(existingMsg);
            }
        }

        // 4. 验证消息内容
        validateMessageContent(req, msgType);

        // 5. 保存消息
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

        // 6. 自动取消隐藏（如果接收方隐藏了会话）
        unhideSessionForReceivers(req.getSessionId(), userId);

        // 7. 更新会话的最后消息 ID 和时间
        ChatSession session = chatSessionService.getById(req.getSessionId());
        session.setLastMsgId(message.getId());
        session.setLastMsgTime(message.getSendTime());
        session.setUpdatedTime(LocalDateTime.now());
        chatSessionService.updateById(session);

        // 8. 写入幂等 Key
        redisTemplate.opsForValue().set(idempotentKey, message.getId(),
                ChatRedisKeys.CHAT_MESSAGE_IDEMPOTENT_EXPIRE, TimeUnit.SECONDS);

        // 9. 发布新消息事件（事务提交后会触发 WebSocket 通知）
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
        // 验证用户是会话成员
        Assert.isTrue(isSessionMember(req.getSessionId(), userId), "您不是该会话的成员");

        // 更新成员的 lastReadId
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

    /**
     * 删除或退出会话
     * <p>
     * 一对一会话：设置 hiddenStatus=1（隐藏但保留成员关系）
     * 群聊会话：设置 deletedStatus=1（真正退出），如果是 OWNER 则移交权限
     *
     * @param req    请求
     * @param userId 当前用户 ID
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteOrLeaveSession(DeleteSessionReq req, String userId) {
        // 1. 验证用户是会话成员
        Assert.isTrue(isSessionMember(req.getSessionId(), userId), "您不是该会话的成员");

        // 2. 查询会话类型
        ChatSession session = chatSessionService.getById(req.getSessionId());
        Assert.notNull(session, "会话不存在");

        ChatSessionTypeEnum sessionType = ChatSessionTypeEnum.parseFromCodeValue(session.getType());
        Assert.notNull(sessionType, "会话类型无效");

        // 3. 查询当前用户的成员记录
        LambdaQueryWrapper<ChatSessionMember> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ChatSessionMember::getSessionId, req.getSessionId())
                .eq(ChatSessionMember::getUserId, userId);

        ChatSessionMember currentMember = chatSessionMemberMapper.selectOne(queryWrapper);
        Assert.notNull(currentMember, "成员记录不存在");

        // 4. 根据会话类型处理
        if (sessionType == ChatSessionTypeEnum.SINGLE) {
            // 一对一会话：隐藏会话
            currentMember.setHiddenStatus((short) 1);
            currentMember.setUpdatedTime(LocalDateTime.now());
            chatSessionMemberService.updateById(currentMember);
            log.info("隐藏一对一会话: sessionId={}, userId={}", req.getSessionId(), userId);
        } else {
            // 群聊会话：退出会话
            boolean isOwner = ChatMemberRoleEnum.OWNER.getCodeValue().equals(currentMember.getRole());

            // 软删除当前成员
            currentMember.setDeletedStatus((short) 1);
            currentMember.setUpdatedTime(LocalDateTime.now());
            chatSessionMemberService.updateById(currentMember);

            // 如果是 OWNER，移交权限
            if (isOwner) {
                transferOwnership(req.getSessionId());
            }

            log.info("退出群聊会话: sessionId={}, userId={}, isOwner={}", req.getSessionId(), userId, isOwner);
        }
    }

    // ========== 私有方法 ==========

    /**
     * 查找现有单聊会话（会话复用逻辑）
     */
    private ChatSession findExistingSingleSession(String configId, String bizType, String bizId, String userId, String otherUserId) {
        // 1. 查询当前用户参与的所有会话
        LambdaQueryWrapper<ChatSessionMember> memberQuery = new LambdaQueryWrapper<>();
        memberQuery.eq(ChatSessionMember::getUserId, userId);

        List<ChatSessionMember> myMembers = chatSessionMemberMapper.selectList(memberQuery);
        if (myMembers.isEmpty()) {
            return null;
        }

        List<String> mySessionIds = myMembers.stream()
                .map(ChatSessionMember::getSessionId)
                .toList();

        // 2. 查询匹配 config + bizType + bizId 的会话
        LambdaQueryWrapper<ChatSession> sessionQuery = new LambdaQueryWrapper<>();
        sessionQuery.in(ChatSession::getId, mySessionIds)
                .eq(ChatSession::getConfigId, configId)
                .eq(ChatSession::getType, ChatSessionTypeEnum.SINGLE.getCodeValue());

        if (StringUtils.hasText(bizType)) {
            sessionQuery.eq(ChatSession::getBizType, bizType);
        } else {
            sessionQuery.isNull(ChatSession::getBizType);
        }

        if (StringUtils.hasText(bizId)) {
            sessionQuery.eq(ChatSession::getBizId, bizId);
        } else {
            sessionQuery.isNull(ChatSession::getBizId);
        }

        List<ChatSession> candidateSessions = chatSessionService.list(sessionQuery);

        // 3. 逐个匹配成员集合
        Set<String> expectedMembers = new HashSet<>(Arrays.asList(userId, otherUserId));

        for (ChatSession session : candidateSessions) {
            LambdaQueryWrapper<ChatSessionMember> membersQuery = new LambdaQueryWrapper<>();
            membersQuery.eq(ChatSessionMember::getSessionId, session.getId());

            List<ChatSessionMember> members = chatSessionMemberMapper.selectList(membersQuery);
            Set<String> actualMembers = members.stream()
                    .map(ChatSessionMember::getUserId)
                    .collect(Collectors.toSet());

            if (expectedMembers.equals(actualMembers)) {
                return session;
            }
        }

        return null;
    }

    /**
     * 创建会话成员
     */
    private void createSessionMembers(String sessionId, String creatorUserId, CreateSessionReq req, ChatSessionTypeEnum sessionType) {
        List<String> memberUserIds = new ArrayList<>();

        if (sessionType == ChatSessionTypeEnum.SINGLE) {
            memberUserIds.add(creatorUserId);
            memberUserIds.add(req.getOtherUserId());
        } else {
            memberUserIds.add(creatorUserId);
            memberUserIds.addAll(req.getMemberUserIds());
        }

        // 去重
        memberUserIds = memberUserIds.stream().distinct().toList();

        for (int i = 0; i < memberUserIds.size(); i++) {
            String memberId = memberUserIds.get(i);
            ChatSessionMember member = new ChatSessionMember();
            member.setId(IdUtil.getSnowflakeNextIdStr());
            member.setSessionId(sessionId);
            member.setUserId(memberId);
            member.setRole(i == 0 ? ChatMemberRoleEnum.OWNER.getCodeValue() : ChatMemberRoleEnum.MEMBER.getCodeValue());
            member.setJoinTime(LocalDateTime.now());

            chatSessionMemberService.save(member);
        }
    }

    /**
     * 检查用户是否是会话成员
     */
    private boolean isSessionMember(String sessionId, String userId) {
        LambdaQueryWrapper<ChatSessionMember> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ChatSessionMember::getSessionId, sessionId)
                .eq(ChatSessionMember::getUserId, userId);

        return chatSessionMemberMapper.selectCount(queryWrapper) > 0;
    }

    /**
     * 自动取消接收方的会话隐藏状态
     * <p>
     * 当发送消息时，如果接收方隐藏了会话，自动将其 hiddenStatus 设为 0
     *
     * @param sessionId 会话 ID
     * @param senderId  发送者用户 ID
     */
    private void unhideSessionForReceivers(String sessionId, String senderId) {
        LambdaQueryWrapper<ChatSessionMember> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ChatSessionMember::getSessionId, sessionId)
                .ne(ChatSessionMember::getUserId, senderId)
                .eq(ChatSessionMember::getHiddenStatus, 1);

        List<ChatSessionMember> hiddenMembers = chatSessionMemberMapper.selectList(queryWrapper);

        if (!hiddenMembers.isEmpty()) {
            for (ChatSessionMember member : hiddenMembers) {
                member.setHiddenStatus((short) 0);
                member.setUpdatedTime(LocalDateTime.now());
                chatSessionMemberService.updateById(member);
            }
            log.info("自动取消会话隐藏: sessionId={}, unhiddenCount={}", sessionId, hiddenMembers.size());
        }
    }

    /**
     * 移交 OWNER 权限给下一个成员
     * <p>
     * 按照 joinTime ASC, userId ASC 排序，选择第一个未删除的成员
     *
     * @param sessionId 会话 ID
     */
    private void transferOwnership(String sessionId) {
        // 查询所有未删除的成员（按加入时间和用户 ID 排序）
        LambdaQueryWrapper<ChatSessionMember> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ChatSessionMember::getSessionId, sessionId)
                .orderByAsc(ChatSessionMember::getJoinTime, ChatSessionMember::getUserId);

        List<ChatSessionMember> remainingMembers = chatSessionMemberMapper.selectList(queryWrapper);

        if (remainingMembers.isEmpty()) {
            log.warn("群聊会话无剩余成员，无法移交 OWNER: sessionId={}", sessionId);
            return;
        }

        // 选择第一个成员作为新的 OWNER
        ChatSessionMember newOwner = remainingMembers.getFirst();
        newOwner.setRole(ChatMemberRoleEnum.OWNER.getCodeValue());
        newOwner.setUpdatedTime(LocalDateTime.now());
        chatSessionMemberService.updateById(newOwner);

        log.info("OWNER 权限已移交: sessionId={}, newOwnerId={}", sessionId, newOwner.getUserId());
    }

    /**
     * 计算未读数
     */
    private Integer calculateUnreadCount(String sessionId, String lastReadId, String userId) {
        if (!StringUtils.hasText(lastReadId)) {
            // 如果从未读过，计算所有消息
            LambdaQueryWrapper<ChatMessage> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(ChatMessage::getSessionId, sessionId)
                    .ne(ChatMessage::getFormUserId, userId);
            return Math.toIntExact(chatMessageMapper.selectCount(queryWrapper));
        }

        LambdaQueryWrapper<ChatMessage> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ChatMessage::getSessionId, sessionId)
                .gt(ChatMessage::getId, lastReadId)
                .ne(ChatMessage::getFormUserId, userId);

        return Math.toIntExact(chatMessageMapper.selectCount(queryWrapper));
    }

    /**
     * 验证消息内容
     */
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

    /**
     * 构建会话响应
     */
    private ChatSessionResp buildSessionResp(ChatSession session, ChatConfig config) {
        ChatSessionResp resp = new ChatSessionResp(session);
        resp.setConfigKey(config.getKey());
        resp.setConfigName(config.getName());
        return resp;
    }

    /**
     * 构建消息响应（不含发送者信息）
     * <p>
     * 用于会话列表查询等场景
     */
    private ChatMessageResp buildMessageResp(ChatMessage message) {
        return buildMessageResp(message, null);
    }

    /**
     * 构建消息响应（含发送者信息）
     *
     * @param message   消息
     * @param senderMap 发送者信息 Map（可选）
     */
    private ChatMessageResp buildMessageResp(ChatMessage message, Map<String, SystemUser> senderMap) {
        ChatMessageResp resp = new ChatMessageResp(message);

        // 填充发送者信息
        if (senderMap != null && StringUtils.hasText(message.getFormUserId())) {
            SystemUser sender = senderMap.get(message.getFormUserId());
            if (sender != null) {
                resp.setFromUserName(sender.getNickname());
                // TODO: 如果 SystemUser 未来增加头像字段，可以在这里填充
                // resp.setFromUserAvatar(sender.getAvatar());
            }
        }

        // 加载卡片数据
        if (ChatMessageTypeEnum.CARD.getCodeValue().equals(message.getMsgType())
                && StringUtils.hasText(message.getCardType())
                && StringUtils.hasText(message.getCardDataId())) {
            Object cardData = chatCardDataService.getCardData(message.getCardType(), message.getCardDataId());
            resp.setCardData(cardData);
        }

        return resp;
    }

    /**
     * 构建发送消息结果响应
     */
    private SendMessageResultResp buildSendResultResp(ChatMessage message) {
        SendMessageResultResp resp = new SendMessageResultResp();
        resp.setMessageId(message.getId());
        resp.setSessionId(message.getSessionId());
        resp.setSendTime(message.getSendTime());
        return resp;
    }

    /**
     * 生成消息摘要
     *
     * @param message 消息
     * @return 消息摘要
     */
    private String generateMessagePreview(ChatMessage message) {
        if (message == null) {
            return "";
        }

        ChatMessageTypeEnum msgType = ChatMessageTypeEnum.parseFromCodeValue(message.getMsgType());
        if (msgType == null) {
            return "";
        }

        return switch (msgType) {
            case TEXT -> {
                // 文本消息：返回前 50 个字符
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

}
