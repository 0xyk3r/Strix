package cn.projectan.strix.core.aop.aspect;

import cn.hutool.core.date.LocalDateTimeUtil;
import cn.hutool.http.useragent.UserAgent;
import cn.hutool.http.useragent.UserAgentUtil;
import cn.projectan.strix.core.ret.RetCode;
import cn.projectan.strix.core.ret.RetResult;
import cn.projectan.strix.model.annotation.SysLog;
import cn.projectan.strix.model.db.SystemLog;
import cn.projectan.strix.model.db.SystemManager;
import cn.projectan.strix.model.properties.StrixLogProperties;
import cn.projectan.strix.utils.SecurityUtils;
import cn.projectan.strix.utils.ServletUtils;
import cn.projectan.strix.utils.async.AsyncFactory;
import cn.projectan.strix.utils.async.AsyncUtil;
import cn.projectan.strix.utils.ip.IpUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.core.NamedThreadLocal;
import org.springframework.stereotype.Component;

/**
 * @author 安炯奕
 * @date 2023/6/17 14:21
 */
@Slf4j
@Aspect
@Component
@EnableConfigurationProperties(StrixLogProperties.class)
@ConditionalOnProperty(prefix = "strix.log", name = "enable", havingValue = "true")
public class SystemLogAspect {

    private static final ThreadLocal<Long> TIME_THREADLOCAL = new NamedThreadLocal<Long>("Spend Time");

    @Value("${spring.application.name}")
    private String applicationName;

    @Autowired
    private ObjectMapper objectMapper;


    /**
     * 请求前执行
     */
    @Before(value = "@annotation(sysLog)")
    public void boBefore(JoinPoint joinPoint, SysLog sysLog) {
        TIME_THREADLOCAL.set(System.currentTimeMillis());
    }

    /**
     * 请求后执行
     *
     * @param joinPoint 切点
     */
    @AfterReturning(pointcut = "@annotation(sysLog)", returning = "retResult")
    public void doAfterReturning(JoinPoint joinPoint, SysLog sysLog, Object retResult) {
        handleLog(joinPoint, sysLog, null, retResult);
    }

    /**
     * 请求异常时执行
     *
     * @param joinPoint 切点
     * @param e         异常
     */
    @AfterThrowing(value = "@annotation(sysLog)", throwing = "e")
    public void doAfterThrowing(JoinPoint joinPoint, SysLog sysLog, Exception e) {
        handleLog(joinPoint, sysLog, e, null);
    }

    protected void handleLog(final JoinPoint joinPoint, SysLog sysLog, final Exception e, Object retResult) {
        try {
            SystemLog systemLog = new SystemLog();
            systemLog.setAppId(applicationName);
            systemLog.setAppVersion("1.0.0");

            // 基于 Request 的信息
            HttpServletRequest request = ServletUtils.getRequest();
            systemLog.setOperationMethod(request.getMethod());
            systemLog.setOperationUrl(request.getRequestURI());

            // 注解上的信息
            systemLog.setOperationType(sysLog.operationType());
            systemLog.setOperationGroup(sysLog.operationGroup());
            systemLog.setOperationName(sysLog.operationName());
            // 请求参数
            if (sysLog.saveRequestParam()) {
                if (request.getMethod().equals("GET")) {
                    systemLog.setOperationParam(objectMapper.writeValueAsString(ServletUtils.getRequestParams(request)));
                } else {
                    systemLog.setOperationParam(objectMapper.writeValueAsString(joinPoint.getArgs()));
                }
            }
            // 响应参数
            systemLog.setResponseCode(e == null ? RetCode.SUCCESS : RetCode.SERVER_ERROR);
            systemLog.setResponseMsg(e == null ? null : e.getMessage());
            if (sysLog.saveResponseData()) {
                if (retResult instanceof RetResult<?> result) {
                    systemLog.setResponseCode(result.getCode());
                    systemLog.setResponseMsg(result.getMsg());
                    systemLog.setResponseData(objectMapper.writeValueAsString(result.getData()));
                } else {
                    systemLog.setResponseData(objectMapper.writeValueAsString(retResult));
                }
            }

            // 客户端信息
            systemLog.setClientIp(IpUtils.getIpAddr(request));
            UserAgent UA = UserAgentUtil.parse(request.getHeader("User-Agent"));
            systemLog.setClientDevice(UA.getOs().getName());

            // 当前登录用户信息
            SystemManager systemManager = SecurityUtils.getSystemManager();
            systemLog.setClientUser(systemManager == null ? null : systemManager.getId());
            systemLog.setClientUsername(systemManager == null ? null : systemManager.getNickname());

            // 时间信息
            long startTime = TIME_THREADLOCAL.get();
            systemLog.setOperationTime(LocalDateTimeUtil.of(startTime));
            systemLog.setOperationSpend(System.currentTimeMillis() - startTime);

            // 保存数据库
            AsyncUtil.instance().execute(AsyncFactory.saveSystemLog(systemLog));
        } catch (Exception exp) {
            log.warn("SystemLogAspect 异常信息: " + exp.getMessage(), exp);
        } finally {
            TIME_THREADLOCAL.remove();
        }
    }

}
