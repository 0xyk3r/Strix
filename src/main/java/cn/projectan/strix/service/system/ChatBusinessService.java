package cn.projectan.strix.service.system;

import cn.hutool.core.util.IdUtil;
import cn.projectan.strix.mapper.system.ChatMessageMapper;
import cn.projectan.strix.mapper.system.ChatSessionMemberMapper;
import cn.projectan.strix.model.db.system.*;
import cn.projectan.strix.model.enums.system.ChatMemberRoleEnum;
import cn.projectan.strix.model.enums.system.ChatSessionTypeEnum;
import cn.projectan.strix.model.event.ChatSessionCreatedEvent;
import cn.projectan.strix.model.request.srv.chat.CreateSessionReq;
import cn.projectan.strix.model.request.srv.chat.DeleteSessionReq;
import cn.projectan.strix.model.request.srv.chat.SessionListReq;
import cn.projectan.strix.model.response.srv.chat.ChatSessionListItemResp;
import cn.projectan.strix.model.response.srv.chat.ChatSessionResp;
import cn.projectan.strix.util.common.I18nUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 聊天会话业务服务
 * <p>
 * 会话创建、查询、删除/退出等操作
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
    private final ChatMessageBusinessService chatMessageBusinessService;
    private final ChatSessionMemberMapper chatSessionMemberMapper;
    private final ChatMessageMapper chatMessageMapper;
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
        Assert.notNull(config, I18nUtil.notFound("field.chatConfig"));

        ChatSessionTypeEnum sessionType = ChatSessionTypeEnum.parseFromCodeValue(config.getSessionType());
        Assert.notNull(sessionType, I18nUtil.invalid("field.chatConfigType"));

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

        // 5. 收集一对一会话中对方用户 ID（用于用户信息查询和在线状态查询）
        List<String> otherUserIds = collectOtherUserIds(sessionPage.getRecords(), sessionMembersMap, userId);

        // 6. 批量查询最后消息（提前查询以收集发送者 ID）
        List<String> lastMsgIds = sessionPage.getRecords().stream()
                .map(ChatSession::getLastMsgId)
                .filter(StringUtils::hasText)
                .toList();

        Map<String, ChatMessage> lastMessageMap = lastMsgIds.isEmpty()
                ? Collections.emptyMap()
                : chatMessageService.listByIds(lastMsgIds).stream()
                .collect(Collectors.toMap(ChatMessage::getId, m -> m));

        // 7. 批量查询用户信息（对方用户 + 最后消息发送者）
        Set<String> userIdsToQuery = new HashSet<>(otherUserIds);
        lastMessageMap.values().stream()
                .map(ChatMessage::getFormUserId)
                .filter(StringUtils::hasText)
                .forEach(userIdsToQuery::add);

        Map<String, SystemUser> userMap = userIdsToQuery.isEmpty()
                ? Collections.emptyMap()
                : systemUserService.listByIds(userIdsToQuery).stream()
                .collect(Collectors.toMap(SystemUser::getId, u -> u));

        // 8. 批量查询在线状态（仅一对一会话的对方）
        Map<String, Boolean> onlineStatusMap = userOnlineStatusService.batchGetOnlineStatus(otherUserIds);

        // 9. 预加载聊天配置（避免循环中逐条查询）
        Set<String> configIds = sessionPage.getRecords().stream()
                .map(ChatSession::getConfigId)
                .filter(StringUtils::hasText)
                .collect(Collectors.toSet());
        Map<String, ChatConfig> configMap = configIds.isEmpty()
                ? Collections.emptyMap()
                : chatConfigService.listByIds(configIds).stream()
                .collect(Collectors.toMap(ChatConfig::getId, c -> c));

        // 10. 构建响应
        Page<ChatSessionListItemResp> respPage = new Page<>(req.getPageIndex(), req.getPageSize(), sessionPage.getTotal());

        Map<String, ChatSessionMember> memberMap = members.stream()
                .collect(Collectors.toMap(ChatSessionMember::getSessionId, m -> m));

        List<ChatSessionListItemResp> respList = sessionPage.getRecords().stream()
                .map(session -> buildSessionListItem(session, userId, sessionMembersMap, userMap, onlineStatusMap, lastMessageMap, memberMap, configMap))
                .toList();

        respPage.setRecords(new ArrayList<>(respList));
        return respPage;
    }

    /**
     * 构建单个会话列表项响应
     */
    private ChatSessionListItemResp buildSessionListItem(
            ChatSession session, String userId,
            Map<String, List<ChatSessionMember>> sessionMembersMap,
            Map<String, SystemUser> userMap,
            Map<String, Boolean> onlineStatusMap,
            Map<String, ChatMessage> lastMessageMap,
            Map<String, ChatSessionMember> memberMap,
            Map<String, ChatConfig> configMap) {

        ChatSessionListItemResp resp = new ChatSessionListItemResp();
        ChatConfig config = configMap.get(session.getConfigId());

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
                            resp.setOtherUserOnlineStatus(onlineStatusMap.getOrDefault(member.getUserId(), false));
                        }
                        break;
                    }
                }
            }
        } else {
            resp.setSessionDisplayName(session.getGroupName());
        }

        // 设置最后消息预览和发送者名称
        if (StringUtils.hasText(session.getLastMsgId())) {
            ChatMessage lastMsg = lastMessageMap.get(session.getLastMsgId());
            if (lastMsg != null) {
                resp.setLastMessagePreview(chatMessageBusinessService.generateMessagePreview(lastMsg));
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

        return resp;
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
        Assert.notNull(session, I18nUtil.notFound("field.session"));

        ChatSessionTypeEnum sessionType = ChatSessionTypeEnum.parseFromCodeValue(session.getType());
        Assert.notNull(sessionType, I18nUtil.invalid("field.sessionType"));

        // 3. 查询当前用户的成员记录
        LambdaQueryWrapper<ChatSessionMember> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ChatSessionMember::getSessionId, req.getSessionId())
                .eq(ChatSessionMember::getUserId, userId);

        ChatSessionMember currentMember = chatSessionMemberMapper.selectOne(queryWrapper);
        Assert.notNull(currentMember, I18nUtil.notFound("field.memberRecord"));

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
     * 收集一对一会话中对方用户 ID
     */
    private List<String> collectOtherUserIds(List<ChatSession> sessions,
                                             Map<String, List<ChatSessionMember>> sessionMembersMap,
                                             String currentUserId) {
        List<String> otherUserIds = new ArrayList<>();
        for (ChatSession session : sessions) {
            if (ChatSessionTypeEnum.SINGLE.getCodeValue().equals(session.getType())) {
                List<ChatSessionMember> sessionMembers = sessionMembersMap.get(session.getId());
                if (sessionMembers != null) {
                    for (ChatSessionMember member : sessionMembers) {
                        if (!member.getUserId().equals(currentUserId)) {
                            otherUserIds.add(member.getUserId());
                        }
                    }
                }
            }
        }
        return otherUserIds;
    }

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

        LocalDateTime now = LocalDateTime.now();
        List<ChatSessionMember> members = new ArrayList<>();
        for (int i = 0; i < memberUserIds.size(); i++) {
            String memberId = memberUserIds.get(i);
            ChatSessionMember member = new ChatSessionMember();
            member.setId(IdUtil.getSnowflakeNextIdStr());
            member.setSessionId(sessionId);
            member.setUserId(memberId);
            member.setRole(i == 0 ? ChatMemberRoleEnum.OWNER.getCodeValue() : ChatMemberRoleEnum.MEMBER.getCodeValue());
            member.setJoinTime(now);
            members.add(member);
        }
        chatSessionMemberService.saveBatch(members);
    }

    /**
     * 检查用户是否是会话成员
     */
    private boolean isSessionMember(String sessionId, String userId) {
        LambdaQueryWrapper<ChatSessionMember> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ChatSessionMember::getSessionId, sessionId)
                .eq(ChatSessionMember::getUserId, userId);

        return chatSessionMemberMapper.exists(queryWrapper);
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
     * 构建会话响应
     */
    private ChatSessionResp buildSessionResp(ChatSession session, ChatConfig config) {
        ChatSessionResp resp = new ChatSessionResp(session);
        resp.setConfigKey(config.getKey());
        resp.setConfigName(config.getName());
        return resp;
    }

}
