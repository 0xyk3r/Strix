package cn.projectan.strix.core.aop.aspect;

import cn.hutool.core.date.LocalDateTimeUtil;
import cn.projectan.strix.config.ApplicationVersionConfig;
import cn.projectan.strix.config.JacksonConfig;
import cn.projectan.strix.core.aop.serializer.SensitiveFieldSerializerModifier;
import cn.projectan.strix.core.aop.serializer.SensitiveMapSerializer;
import cn.projectan.strix.core.exception.StrixNoAuthException;
import cn.projectan.strix.core.ret.RetCode;
import cn.projectan.strix.core.ret.RetResult;
import cn.projectan.strix.model.annotation.StrixLog;
import cn.projectan.strix.model.db.system.SystemLog;
import cn.projectan.strix.model.db.system.SystemManager;
import cn.projectan.strix.model.other.system.ua.UserAgent;
import cn.projectan.strix.model.properties.system.StrixLogProperties;
import cn.projectan.strix.service.system.AsyncSystemLogService;
import cn.projectan.strix.util.http.ServletUtils;
import cn.projectan.strix.util.ip.IpUtils;
import cn.projectan.strix.util.system.SecurityUtils;
import cn.projectan.strix.util.ua.UserAgentUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.core.NamedThreadLocal;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestMethod;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.module.SimpleModule;

import java.util.Map;

/**
 * 系统日志切面
 * <p>
 * 1. 使用 SensitiveFieldSerializerModifier 对 Bean 对象敏感字段进行脱敏
 * 2. 使用 SensitiveMapSerializer 对 Map 类型参数（如 GET 请求）进行脱敏
 * 3. 异常处理，区分不同类型的异常
 * 4. 使用批量异步保存，提升性能
 * 5. 从配置中读取应用版本号
 * </p>
 *
 * @author ProjectAn
 * @since 2023/6/17 14:21
 */
@Slf4j
@Aspect
@Component
@EnableConfigurationProperties(StrixLogProperties.class)
@ConditionalOnProperty(prefix = "strix.log", name = "enable", havingValue = "true")
public class SystemLogAspect {

    private static final ThreadLocal<Long> TIME_THREADLOCAL = new NamedThreadLocal<>("Spend Time");

    private final ObjectMapper objectMapper;
    private final ApplicationVersionConfig versionConfig;
    private final AsyncSystemLogService asyncSystemLogService;

    public SystemLogAspect(ApplicationVersionConfig versionConfig, AsyncSystemLogService asyncSystemLogService) {
        this.versionConfig = versionConfig;
        this.asyncSystemLogService = asyncSystemLogService;

        // 配置敏感字段脱敏的 Jackson 序列化模块
        SimpleModule sensitiveModule = new SimpleModule("SensitiveDataModule");
        sensitiveModule.setSerializerModifier(new SensitiveFieldSerializerModifier());
        sensitiveModule.addSerializer(Map.class, new SensitiveMapSerializer());

        this.objectMapper = JacksonConfig.builder()
                .addModule(sensitiveModule)
                .build();
    }

    /**
     * 请求前执行
     */
    @Before(value = "@annotation(strixLog)")
    public void doBefore(JoinPoint joinPoint, StrixLog strixLog) {
        TIME_THREADLOCAL.set(System.currentTimeMillis());
    }

    /**
     * 请求后执行
     *
     * @param joinPoint 切点
     * @param strixLog  日志注解
     * @param retResult 返回结果
     */
    @AfterReturning(pointcut = "@annotation(strixLog)", returning = "retResult")
    public void doAfterReturning(JoinPoint joinPoint, StrixLog strixLog, Object retResult) {
        handleLog(joinPoint, strixLog, null, retResult);
    }

    /**
     * 请求异常时执行
     *
     * @param joinPoint 切点
     * @param strixLog  日志注解
     * @param e         异常
     */
    @AfterThrowing(value = "@annotation(strixLog)", throwing = "e")
    public void doAfterThrowing(JoinPoint joinPoint, StrixLog strixLog, Exception e) {
        handleLog(joinPoint, strixLog, e, null);
    }

    /**
     * 处理日志记录
     */
    protected void handleLog(final JoinPoint joinPoint, StrixLog strixLog, final Exception e, Object retResult) {
        try {
            // 检查 ThreadLocal 是否有值（防止只执行 After 未执行 Before 的情况）
            Long startTime = TIME_THREADLOCAL.get();
            if (startTime == null) {
                log.warn("TIME_THREADLOCAL is null, skip log recording");
                return;
            }

            SystemLog systemLog = new SystemLog();

            // 应用信息
            systemLog.setAppId(versionConfig.getApplicationName());
            systemLog.setAppVersion(versionConfig.getVersion());

            // 基于 Request 的信息
            HttpServletRequest request = ServletUtils.getRequest();
            systemLog.setOperationMethod(request.getMethod());
            systemLog.setOperationUrl(request.getRequestURI());

            // 注解上的信息
            systemLog.setOperationType(strixLog.operationType());
            systemLog.setOperationGroup(strixLog.operationGroup());
            systemLog.setOperationName(strixLog.operationName());

            // 请求参数（使用安全的序列化器，自动过滤敏感信息）
            if (strixLog.saveRequestParam()) {
                try {
                    if (RequestMethod.GET.name().equals(request.getMethod())) {
                        systemLog.setOperationParam(
                                objectMapper.writeValueAsString(ServletUtils.getRequestParams(request))
                        );
                    } else {
                        systemLog.setOperationParam(
                                objectMapper.writeValueAsString(joinPoint.getArgs())
                        );
                    }
                } catch (Exception ex) {
                    log.warn("Failed to serialize request params: {}", ex.getMessage());
                    systemLog.setOperationParam("[Serialization Failed]");
                }
            }

            // 响应参数
            handleResponse(systemLog, strixLog, e, retResult);

            // 客户端信息
            systemLog.setClientIp(IpUtils.getIpAddr(request));
            handleUserAgent(systemLog, request);

            // 当前登录用户信息
            try {
                SystemManager systemManager = SecurityUtils.getSystemManager();
                systemLog.setClientUser(systemManager == null ? null : systemManager.getId());
                systemLog.setClientUsername(systemManager == null ? null : systemManager.getNickname());
            } catch (Exception ex) {
                log.debug("Failed to get system manager: {}", ex.getMessage());
            }

            // 时间信息
            systemLog.setOperationTime(LocalDateTimeUtil.of(startTime));
            systemLog.setOperationSpend(System.currentTimeMillis() - startTime);

            // 异步保存到数据库（使用批量插入）
            asyncSystemLogService.saveAsync(systemLog);
        } catch (Exception exp) {
            log.warn("SystemLogAspect 异常信息: {}", exp.getMessage(), exp);
        } finally {
            TIME_THREADLOCAL.remove();
        }
    }

    /**
     * 处理响应信息
     */
    private void handleResponse(SystemLog systemLog, StrixLog strixLog, Exception e, Object retResult) {
        if (e != null) {
            // 异常情况：根据异常类型设置不同的响应码
            switch (e) {
                case StrixNoAuthException ignored -> systemLog.setResponseCode(RetCode.NOT_LOGIN);
                case AccessDeniedException ignored -> systemLog.setResponseCode(RetCode.NOT_PERMISSION);
                default -> systemLog.setResponseCode(RetCode.SERVER_ERROR);
            }
            systemLog.setResponseMsg(e.getMessage());
        } else {
            // 正常情况：始终从 RetResult 中提取实际的响应码和消息
            systemLog.setResponseCode(RetCode.SUCCESS);
            if (retResult instanceof RetResult<?> result) {
                systemLog.setResponseCode(result.getCode());
                systemLog.setResponseMsg(result.getMsg());
            }

            // 按需保存响应体数据
            if (strixLog.saveResponseData() && retResult != null) {
                try {
                    if (retResult instanceof RetResult<?> result) {
                        systemLog.setResponseData(objectMapper.writeValueAsString(result.getData()));
                    } else {
                        systemLog.setResponseData(objectMapper.writeValueAsString(retResult));
                    }
                } catch (Exception ex) {
                    log.warn("Failed to serialize response data: {}", ex.getMessage());
                    systemLog.setResponseData("[Serialization Failed]");
                }
            }
        }
    }

    /**
     * 处理 UserAgent 信息
     */
    private void handleUserAgent(SystemLog systemLog, HttpServletRequest request) {
        try {
            String userAgentHeader = request.getHeader("User-Agent");
            if (userAgentHeader != null && !userAgentHeader.isBlank()) {
                UserAgent ua = UserAgentUtil.parse(userAgentHeader);
                if (ua != null && ua.getOs() != null) {
                    systemLog.setClientDevice(ua.getOs().getName());
                } else {
                    systemLog.setClientDevice("Unknown");
                }
            } else {
                systemLog.setClientDevice("Unknown");
            }
        } catch (Exception ex) {
            log.debug("Failed to parse User-Agent: {}", ex.getMessage());
            systemLog.setClientDevice("Unknown");
        }
    }

}
