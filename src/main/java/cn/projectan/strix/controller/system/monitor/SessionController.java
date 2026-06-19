package cn.projectan.strix.controller.system.monitor;

import cn.projectan.strix.controller.system.base.BaseSystemController;
import cn.projectan.strix.core.ret.RetBuilder;
import cn.projectan.strix.core.ret.RetResult;
import cn.projectan.strix.model.db.system.SystemManager;
import cn.projectan.strix.model.response.system.monitor.session.OnlineSessionResp;
import cn.projectan.strix.model.response.system.monitor.session.OnlineSessionResp.OnlineSessionItem;
import cn.projectan.strix.model.response.system.monitor.session.SessionMeta;
import cn.projectan.strix.service.system.SystemManagerService;
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
 * 在线会话管理
 *
 * @author ProjectAn
 */
@Slf4j
@RestController
@RequestMapping("system/monitor/session")
@RequiredArgsConstructor
@Tag(name = "系统监控 - 在线会话管理")
public class SessionController extends BaseSystemController {

    private final TokenSessionService tokenSessionService;
    private final SystemManagerService systemManagerService;

    @GetMapping("")
    @PreAuthorize("@ss.hasPermission('system:monitor:session')")
    @Operation(summary = "在线会话列表")
    public RetResult<OnlineSessionResp> list(@RequestParam(required = false) String keyword) {
        Set<String> onlineManagerIds = tokenSessionService.getOnlineManagerIds();
        List<OnlineSessionItem> items = new ArrayList<>();
        int totalSessions = 0;

        // 批量查询所有在线管理员，避免 N+1 查询
        Map<String, SystemManager> managerMap = systemManagerService.listByIds(onlineManagerIds)
                .stream()
                .collect(Collectors.toMap(SystemManager::getId, m -> m));

        for (String managerId : onlineManagerIds) {
            SystemManager manager = managerMap.get(managerId);
            if (manager == null) {
                continue;
            }

            if (StringUtils.hasText(keyword)) {
                String kw = keyword.toLowerCase();
                boolean match = (manager.getNickname() != null && manager.getNickname().toLowerCase().contains(kw))
                        || (manager.getLoginName() != null && manager.getLoginName().toLowerCase().contains(kw));
                if (!match) {
                    continue;
                }
            }

            Map<String, SessionMeta> sessions = tokenSessionService.getManagerSessions(managerId);
            int sessionCount = sessions.size();
            totalSessions += sessionCount;

            for (Map.Entry<String, SessionMeta> entry : sessions.entrySet()) {
                String token = entry.getKey();
                SessionMeta meta = entry.getValue();

                OnlineSessionItem item = new OnlineSessionItem();
                item.setManagerId(managerId);
                item.setNickname(manager.getNickname());
                item.setLoginName(manager.getLoginName());
                item.setTokenMasked(maskToken(token));
                item.setLoginTime(meta.getLoginTime());
                item.setLastActiveTime(meta.getLastActiveTime());
                item.setIp(meta.getIp());
                item.setDevice(meta.getDevice());
                item.setSessionCount(sessionCount);
                items.add(item);
            }
        }

        // 按最后活跃时间降序
        items.sort((a, b) -> {
            if (a.getLastActiveTime() == null && b.getLastActiveTime() == null) return 0;
            if (a.getLastActiveTime() == null) return 1;
            if (b.getLastActiveTime() == null) return -1;
            return b.getLastActiveTime().compareTo(a.getLastActiveTime());
        });

        return RetBuilder.success(new OnlineSessionResp(items, onlineManagerIds.size(), totalSessions));
    }

    @PostMapping("kick")
    @PreAuthorize("@ss.hasPermission('system:monitor:session')")
    @Operation(summary = "踢出指定管理员所有会话")
    public RetResult<Void> kick(@RequestBody KickRequest req) {
        tokenSessionService.invalidateManagerSession(req.managerId());
        log.info("管理员 {} 被踢出所有会话", req.managerId());
        return RetBuilder.success();
    }

    @PostMapping("batch-kick")
    @PreAuthorize("@ss.hasPermission('system:monitor:session')")
    @Operation(summary = "批量踢出管理员所有会话")
    public RetResult<Void> batchKick(@RequestBody BatchKickRequest req) {
        for (String managerId : req.managerIds()) {
            tokenSessionService.invalidateManagerSession(managerId);
        }
        log.info("批量踢出 {} 个管理员的所有会话", req.managerIds().size());
        return RetBuilder.success();
    }

    private String maskToken(String token) {
        if (token == null || token.length() <= 8) {
            return "****";
        }
        return token.substring(0, 4) + "****" + token.substring(token.length() - 4);
    }

    record KickRequest(String managerId) {}
    record BatchKickRequest(List<String> managerIds) {}
}
