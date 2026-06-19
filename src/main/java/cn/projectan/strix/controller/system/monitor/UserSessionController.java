package cn.projectan.strix.controller.system.monitor;

import cn.projectan.strix.controller.system.base.BaseSystemController;
import cn.projectan.strix.core.ret.RetBuilder;
import cn.projectan.strix.core.ret.RetResult;
import cn.projectan.strix.model.db.system.SystemUser;
import cn.projectan.strix.model.response.system.monitor.session.OnlineUserSessionResp;
import cn.projectan.strix.model.response.system.monitor.session.OnlineUserSessionResp.OnlineUserSessionItem;
import cn.projectan.strix.model.response.system.monitor.session.SessionMeta;
import cn.projectan.strix.service.system.SystemUserService;
import cn.projectan.strix.service.system.TokenSessionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 在线用户会话管理
 *
 * @author ProjectAn
 */
@Slf4j
@RestController
@RequestMapping("system/monitor/user-session")
@RequiredArgsConstructor
@Tag(name = "系统监控 - 在线用户会话管理")
public class UserSessionController extends BaseSystemController {

    private final TokenSessionService tokenSessionService;
    private final SystemUserService systemUserService;

    @GetMapping("")
    @PreAuthorize("@ss.hasPermission('system:monitor:user-session')")
    @Operation(summary = "在线用户会话列表")
    public RetResult<OnlineUserSessionResp> list(@RequestParam(required = false) String keyword) {
        Set<String> onlineUserIds = tokenSessionService.getOnlineUserIds();
        List<OnlineUserSessionItem> items = new ArrayList<>();
        int totalSessions = 0;

        // 批量查询所有在线用户，避免 N+1 查询
        Map<String, SystemUser> userMap = systemUserService.listByIds(onlineUserIds)
                .stream()
                .collect(Collectors.toMap(SystemUser::getId, u -> u));

        for (String userId : onlineUserIds) {
            SystemUser user = userMap.get(userId);
            if (user == null) {
                continue;
            }

            if (StringUtils.hasText(keyword)) {
                String kw = keyword.toLowerCase();
                boolean match = (user.getNickname() != null && user.getNickname().toLowerCase().contains(kw))
                        || (user.getPhoneNumber() != null && user.getPhoneNumber().contains(kw));
                if (!match) {
                    continue;
                }
            }

            Map<String, SessionMeta> sessions = tokenSessionService.getUserSessions(userId);
            int sessionCount = sessions.size();
            totalSessions += sessionCount;

            for (Map.Entry<String, SessionMeta> entry : sessions.entrySet()) {
                String token = entry.getKey();
                SessionMeta meta = entry.getValue();

                OnlineUserSessionItem item = new OnlineUserSessionItem();
                item.setUserId(userId);
                item.setNickname(user.getNickname());
                item.setPhoneNumber(user.getPhoneNumber());
                item.setTokenMasked(maskToken(token));
                item.setLoginTime(meta.getLoginTime());
                item.setLastActiveTime(meta.getLastActiveTime());
                item.setIp(meta.getIp());
                item.setDevice(meta.getDevice());
                item.setSessionCount(sessionCount);
                items.add(item);
            }
        }

        items.sort((a, b) -> {
            if (a.getLastActiveTime() == null && b.getLastActiveTime() == null) return 0;
            if (a.getLastActiveTime() == null) return 1;
            if (b.getLastActiveTime() == null) return -1;
            return b.getLastActiveTime().compareTo(a.getLastActiveTime());
        });

        return RetBuilder.success(new OnlineUserSessionResp(items, onlineUserIds.size(), totalSessions));
    }

    @PostMapping("kick")
    @PreAuthorize("@ss.hasPermission('system:monitor:user-session')")
    @Operation(summary = "踢出指定用户所有会话")
    public RetResult<Void> kick(@RequestBody KickRequest req) {
        tokenSessionService.invalidateUserSession(req.userId());
        log.info("用户 {} 被踢出所有会话", req.userId());
        return RetBuilder.success();
    }

    @PostMapping("batch-kick")
    @PreAuthorize("@ss.hasPermission('system:monitor:user-session')")
    @Operation(summary = "批量踢出用户所有会话")
    public RetResult<Void> batchKick(@RequestBody BatchKickRequest req) {
        for (String userId : req.userIds()) {
            tokenSessionService.invalidateUserSession(userId);
        }
        log.info("批量踢出 {} 个用户的所有会话", req.userIds().size());
        return RetBuilder.success();
    }

    private String maskToken(String token) {
        if (token == null || token.length() <= 8) {
            return "****";
        }
        return token.substring(0, 4) + "****" + token.substring(token.length() - 4);
    }

    record KickRequest(String userId) {}
    record BatchKickRequest(List<String> userIds) {}
}
