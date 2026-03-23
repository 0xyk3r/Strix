package cn.projectan.strix.model.constant.system;

import org.springframework.core.Ordered;

/**
 * AOP 组件执行顺序常量
 * <p>
 * 统一管理所有切面/拦截器的 @Order 值，避免分散定义导致的顺序冲突。
 *
 * @author ProjectAn
 */
public final class AopOrderConstants {

    private AopOrderConstants() {
    }

    /**
     * API 安全校验切面 — 最高优先级
     */
    public static final int SECURITY_CHECK = Ordered.HIGHEST_PRECEDENCE;

    /**
     * 系统日志切面 — 安全校验之后
     */
    public static final int SYSTEM_LOG = SECURITY_CHECK + 10;
}
