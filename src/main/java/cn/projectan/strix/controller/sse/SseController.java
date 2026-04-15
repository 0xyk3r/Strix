package cn.projectan.strix.controller.sse;

import cn.projectan.strix.controller.BaseController;
import cn.projectan.strix.core.ss.details.LoginSystemManager;
import cn.projectan.strix.core.sse.SseSessionManager;
import cn.projectan.strix.model.annotation.Anonymous;
import cn.projectan.strix.model.annotation.IgnoreEncryption;
import cn.projectan.strix.model.constant.system.LoginRedisKeys;
import cn.projectan.strix.service.system.NotificationReceiverService;
import cn.projectan.strix.util.common.RedisUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;

/**
 * SSE 通用推送端点
 * <p>
 * EventSource API 不支持自定义 Header, 因此通过 query param 传递 token,
 * 使用 @Anonymous 跳过 Spring Security 过滤器, 在方法内部手动验证 token.
 * <p>
 * 支持的事件类型:
 * - notification:new — 新通知
 * - notification:count — 未读通知数量变更
 * - auth:refresh — 权限/菜单变更, 前端应刷新 loginInfo
 *
 * @author ProjectAn
 * @since 2026-03-26
 */
@Slf4j
@RestController
@RequestMapping("sse")
@RequiredArgsConstructor
@IgnoreEncryption
@Tag(name = "SSE - 实时推送")
public class SseController extends BaseController {

    private final SseSessionManager sseSessionManager;
    private final RedisUtil redisUtil;
    private final NotificationReceiverService notificationReceiverService;

    @Anonymous
    @GetMapping(value = "stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "建立 SSE 连接")
    public SseEmitter connect(@RequestParam String token) {
        // 内部验证 token
        Object loginInfo = redisUtil.get(LoginRedisKeys.MANAGER_TOKEN_PREFIX + token);
        if (!(loginInfo instanceof LoginSystemManager lsm) || lsm.getSystemManager() == null) {
            SseEmitter emitter = new SseEmitter(0L);
            try {
                emitter.send(SseEmitter.event().name("error").data(Map.of("message", "Unauthorized")));
            } catch (IOException ignored) {
            }
            emitter.complete();
            return emitter;
        }

        String managerId = lsm.getSystemManager().getId();
        SseEmitter emitter = sseSessionManager.createEmitter(managerId);

        // 发送初始未读数量
        try {
            long unreadCount = notificationReceiverService.getUnreadCountByReceiverId(managerId);
            emitter.send(SseEmitter.event()
                    .name("notification:count")
                    .data(Map.of("unreadCount", unreadCount)));
        } catch (IOException e) {
            log.warn("发送初始未读数量失败: managerId={}", managerId, e);
        }

        log.info("SSE 连接已建立: managerId={}", managerId);
        return emitter;
    }
}
