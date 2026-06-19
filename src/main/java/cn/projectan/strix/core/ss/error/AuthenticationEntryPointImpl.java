package cn.projectan.strix.core.ss.error;

import cn.projectan.strix.config.ApplicationVersionConfig;
import cn.projectan.strix.core.ret.RetBuilder;
import cn.projectan.strix.core.ret.RetCode;
import cn.projectan.strix.model.db.system.SystemLog;
import cn.projectan.strix.model.dict.system.SystemLogOperType;
import cn.projectan.strix.service.system.AsyncSystemLogService;
import cn.projectan.strix.util.common.I18nUtil;
import cn.projectan.strix.util.http.ServletUtil;
import cn.projectan.strix.util.ip.IpUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.time.LocalDateTime;

/**
 * @author ProjectAn
 * @since 2023/2/25 0:53
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AuthenticationEntryPointImpl implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;
    private final AsyncSystemLogService asyncSystemLogService;
    private final ApplicationVersionConfig versionConfig;

    @Override
    public void commence(@NonNull HttpServletRequest request, HttpServletResponse response, @NonNull AuthenticationException authException) throws IOException {
        // 记录认证失败审计日志
        recordSecurityLog(request, authException);

        response.setContentType("application/json;charset=utf-8");
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        ServletUtil.write(response, objectMapper.writeValueAsString(RetBuilder.error(RetCode.NOT_LOGIN, I18nUtil.get("error.notLogin"))));
    }

    private void recordSecurityLog(HttpServletRequest request, AuthenticationException e) {
        try {
            SystemLog systemLog = new SystemLog();
            systemLog.setAppId(versionConfig.getApplicationName());
            systemLog.setAppVersion(versionConfig.getVersion());
            systemLog.setOperationType(SystemLogOperType.SECURITY);
            systemLog.setOperationGroup("身份认证");
            systemLog.setOperationName("认证失败");
            systemLog.setOperationMethod(request.getMethod());
            systemLog.setOperationUrl(request.getRequestURI());
            systemLog.setOperationTime(LocalDateTime.now());
            systemLog.setClientIp(IpUtils.getIpAddr(request));
            systemLog.setResponseCode(RetCode.NOT_LOGIN);
            systemLog.setResponseMsg(e.getMessage());

            asyncSystemLogService.saveAsync(systemLog);
        } catch (Exception ex) {
            log.warn("记录认证失败日志失败: {}", ex.getMessage());
        }
    }

}
