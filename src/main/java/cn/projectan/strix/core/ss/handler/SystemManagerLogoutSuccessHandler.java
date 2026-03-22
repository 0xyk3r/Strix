package cn.projectan.strix.core.ss.handler;

import cn.hutool.http.useragent.UserAgent;
import cn.hutool.http.useragent.UserAgentUtil;
import cn.projectan.strix.core.ret.RetCode;
import cn.projectan.strix.core.ss.details.LoginSystemManager;
import cn.projectan.strix.model.db.system.SystemLog;
import cn.projectan.strix.model.db.system.SystemManager;
import cn.projectan.strix.model.dict.system.SystemLogOperType;
import cn.projectan.strix.service.system.AsyncSystemLogService;
import cn.projectan.strix.service.system.TokenSessionService;
import cn.projectan.strix.util.http.TokenUtil;
import cn.projectan.strix.util.ip.IpUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;

/**
 * @author ProjectAn
 * @since 2023/5/26 17:58
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SystemManagerLogoutSuccessHandler implements LogoutSuccessHandler {

    private final TokenSessionService tokenSessionService;
    private final ObjectProvider<AsyncSystemLogService> asyncSystemLogServiceProvider;

    @Override
    public void onLogoutSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) {
        String token = TokenUtil.resolveToken(request);

        // 在删除 token 前获取用户信息用于审计日志
        SystemManager manager = resolveManager(authentication);

        if (StringUtils.hasText(token)) {
            tokenSessionService.logoutManager(token);
        }

        recordLogoutLog(request, manager);
    }

    private SystemManager resolveManager(Authentication authentication) {
        if (authentication != null && authentication.getPrincipal() instanceof LoginSystemManager lsm) {
            return lsm.getSystemManager();
        }
        return null;
    }

    private void recordLogoutLog(HttpServletRequest request, SystemManager manager) {
        AsyncSystemLogService asyncSystemLogService = asyncSystemLogServiceProvider.getIfAvailable();
        if (asyncSystemLogService == null) {
            return;
        }
        try {
            SystemLog systemLog = new SystemLog();
            systemLog.setOperationType(SystemLogOperType.LOGOUT);
            systemLog.setOperationGroup("系统登出");
            systemLog.setOperationName("系统登出");
            systemLog.setOperationMethod(request.getMethod());
            systemLog.setOperationUrl(request.getRequestURI());
            systemLog.setOperationTime(LocalDateTime.now());
            systemLog.setOperationSpend(0L);
            systemLog.setClientIp(IpUtils.getIpAddr(request));
            systemLog.setResponseCode(RetCode.SUCCESS);
            systemLog.setResponseMsg("登出成功");

            parseUserAgent(systemLog, request);

            if (manager != null) {
                systemLog.setClientUser(manager.getId());
                systemLog.setClientUsername(manager.getNickname());
            }

            asyncSystemLogService.saveAsync(systemLog);
        } catch (Exception e) {
            log.warn("记录登出审计日志失败: {}", e.getMessage(), e);
        }
    }

    private void parseUserAgent(SystemLog systemLog, HttpServletRequest request) {
        try {
            String userAgentHeader = request.getHeader("User-Agent");
            if (userAgentHeader != null && !userAgentHeader.isBlank()) {
                UserAgent ua = UserAgentUtil.parse(userAgentHeader);
                if (ua != null && ua.getOs() != null) {
                    systemLog.setClientDevice(ua.getOs().getName());
                    return;
                }
            }
        } catch (Exception ignored) {
        }
        systemLog.setClientDevice("Unknown");
    }

}
