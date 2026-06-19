package cn.projectan.strix.core.ss.error;

import cn.projectan.strix.config.ApplicationVersionConfig;
import cn.projectan.strix.core.ret.RetBuilder;
import cn.projectan.strix.core.ret.RetCode;
import cn.projectan.strix.model.db.system.SystemLog;
import cn.projectan.strix.model.db.system.SystemManager;
import cn.projectan.strix.model.dict.system.SystemLogOperType;
import cn.projectan.strix.service.system.AsyncSystemLogService;
import cn.projectan.strix.util.common.I18nUtil;
import cn.projectan.strix.util.http.ServletUtil;
import cn.projectan.strix.util.ip.IpUtils;
import cn.projectan.strix.util.system.SecurityUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.time.LocalDateTime;

/**
 * @author ProjectAn
 * @since 2023/2/25 0:52
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AccessDeniedHandlerImpl implements AccessDeniedHandler {

    private final ObjectMapper objectMapper;
    private final AsyncSystemLogService asyncSystemLogService;
    private final ApplicationVersionConfig versionConfig;

    @Override
    public void handle(@NonNull HttpServletRequest request, HttpServletResponse response, @NonNull AccessDeniedException accessDeniedException) throws IOException {
        // 记录权限拒绝审计日志
        recordSecurityLog(request, accessDeniedException);

        response.setContentType("application/json;charset=utf-8");
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        ServletUtil.write(response, objectMapper.writeValueAsString(RetBuilder.error(RetCode.NOT_PERMISSION, I18nUtil.get("error.notPermission"))));
    }

    private void recordSecurityLog(HttpServletRequest request, AccessDeniedException e) {
        try {
            SystemLog systemLog = new SystemLog();
            systemLog.setAppId(versionConfig.getApplicationName());
            systemLog.setAppVersion(versionConfig.getVersion());
            systemLog.setOperationType(SystemLogOperType.SECURITY);
            systemLog.setOperationGroup("访问控制");
            systemLog.setOperationName("权限拒绝");
            systemLog.setOperationMethod(request.getMethod());
            systemLog.setOperationUrl(request.getRequestURI());
            systemLog.setOperationTime(LocalDateTime.now());
            systemLog.setClientIp(IpUtils.getIpAddr(request));
            systemLog.setResponseCode(RetCode.NOT_PERMISSION);
            systemLog.setResponseMsg(e.getMessage());

            try {
                SystemManager manager = SecurityUtil.getSystemManager();
                if (manager != null) {
                    systemLog.setClientUser(manager.getId());
                    systemLog.setClientUsername(manager.getNickname());
                }
            } catch (Exception userEx) {
                log.debug("获取当前登录用户信息失败", userEx);
            }

            asyncSystemLogService.saveAsync(systemLog);
        } catch (Exception ex) {
            log.warn("记录权限拒绝日志失败: {}", ex.getMessage());
        }
    }

}
