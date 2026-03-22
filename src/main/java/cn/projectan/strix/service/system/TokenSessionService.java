package cn.projectan.strix.service.system;

import cn.hutool.core.util.IdUtil;
import cn.projectan.strix.core.ss.details.LoginSystemManager;
import cn.projectan.strix.core.ss.details.LoginSystemUser;
import cn.projectan.strix.model.constant.system.LoginRedisKeys;
import cn.projectan.strix.util.common.RedisUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * Token 会话管理服务
 * <p>
 * 统一管理 SystemManager 和 SystemUser 的 Token 创建、刷新、失效等操作，
 * 避免重复的 Redis Token 操作代码分散在各 Controller/Service 中。
 *
 * @author ProjectAn
 * @since 2026-03-22
 */
@Service
@RequiredArgsConstructor
public class TokenSessionService {

    private final RedisUtil redisUtil;

    // ======================== Manager Session ========================

    /**
     * 使管理员旧 session 失效（删除 id→token 和 token→info 两个键）
     */
    public void invalidateManagerSession(String managerId) {
        invalidateSession(
                LoginRedisKeys.LOGIN_MANAGER_ID_TO_TOKEN_PREFIX + managerId,
                LoginRedisKeys.LOGIN_MANAGER_TOKEN_TO_USER_INFO_PREFIX
        );
    }

    /**
     * 创建管理员 session，返回新 token
     */
    public String createManagerSession(String managerId, LoginSystemManager loginInfo, long ttlMinutes) {
        return createSession(
                LoginRedisKeys.LOGIN_MANAGER_ID_TO_TOKEN_PREFIX + managerId,
                LoginRedisKeys.LOGIN_MANAGER_TOKEN_TO_USER_INFO_PREFIX,
                loginInfo, ttlMinutes
        );
    }

    /**
     * 刷新管理员 session TTL（延长过期时间）
     */
    public void refreshManagerSessionTTL(String managerId, long ttlMinutes) {
        refreshSessionTTL(
                LoginRedisKeys.LOGIN_MANAGER_ID_TO_TOKEN_PREFIX + managerId,
                LoginRedisKeys.LOGIN_MANAGER_TOKEN_TO_USER_INFO_PREFIX,
                ttlMinutes
        );
    }

    /**
     * 根据管理员 ID 获取其当前登录信息（从 Redis 中读取）
     *
     * @return 如果 token 有效则返回 LoginSystemManager，否则返回 null
     */
    public LoginSystemManager getManagerLoginInfoById(String managerId) {
        Object tokenObj = redisUtil.get(LoginRedisKeys.LOGIN_MANAGER_ID_TO_TOKEN_PREFIX + managerId);
        if (tokenObj instanceof String token) {
            Object loginInfo = redisUtil.get(LoginRedisKeys.LOGIN_MANAGER_TOKEN_TO_USER_INFO_PREFIX + token);
            if (loginInfo instanceof LoginSystemManager lsm) {
                return lsm;
            }
        }
        return null;
    }

    /**
     * 刷新管理员登录信息（保持现有 TTL 不变）
     */
    public void refreshManagerLoginInfo(String managerId, LoginSystemManager loginInfo) {
        refreshLoginInfo(
                LoginRedisKeys.LOGIN_MANAGER_ID_TO_TOKEN_PREFIX + managerId,
                LoginRedisKeys.LOGIN_MANAGER_TOKEN_TO_USER_INFO_PREFIX,
                loginInfo
        );
    }

    /**
     * 管理员登出（根据 token 删除所有关联的 session 数据）
     */
    public void logoutManager(String token) {
        Object loginInfo = redisUtil.get(LoginRedisKeys.LOGIN_MANAGER_TOKEN_TO_USER_INFO_PREFIX + token);
        redisUtil.del(LoginRedisKeys.LOGIN_MANAGER_TOKEN_TO_USER_INFO_PREFIX + token);
        if (loginInfo instanceof LoginSystemManager lsm && lsm.getSystemManager() != null) {
            String managerId = lsm.getSystemManager().getId();
            if (managerId != null) {
                redisUtil.del(LoginRedisKeys.LOGIN_MANAGER_ID_TO_TOKEN_PREFIX + managerId);
            }
        }
    }

    // ======================== User Session ========================

    /**
     * 使用户旧 session 失效
     */
    public void invalidateUserSession(String userId) {
        invalidateSession(
                LoginRedisKeys.LOGIN_USER_ID_TO_TOKEN_PREFIX + userId,
                LoginRedisKeys.LOGIN_USER_TOKEN_TO_USER_INFO_PREFIX
        );
    }

    /**
     * 创建用户 session，返回新 token
     */
    public String createUserSession(String userId, Object loginInfo, long ttlMinutes) {
        return createSession(
                LoginRedisKeys.LOGIN_USER_ID_TO_TOKEN_PREFIX + userId,
                LoginRedisKeys.LOGIN_USER_TOKEN_TO_USER_INFO_PREFIX,
                loginInfo, ttlMinutes
        );
    }

    /**
     * 获取或刷新用户 session：如果已有有效 token 则刷新 TTL 并返回，否则返回 null
     */
    public String getOrRefreshUserSession(String userId, LoginSystemUser loginInfo, long ttlMinutes) {
        Object existingTokenObj = redisUtil.get(LoginRedisKeys.LOGIN_USER_ID_TO_TOKEN_PREFIX + userId);
        if (existingTokenObj instanceof String existingToken) {
            Object cachedObj = redisUtil.get(LoginRedisKeys.LOGIN_USER_TOKEN_TO_USER_INFO_PREFIX + existingToken);
            if (cachedObj instanceof LoginSystemUser) {
                redisUtil.set(LoginRedisKeys.LOGIN_USER_TOKEN_TO_USER_INFO_PREFIX + existingToken, loginInfo, ttlMinutes, TimeUnit.MINUTES);
                redisUtil.setExpire(LoginRedisKeys.LOGIN_USER_ID_TO_TOKEN_PREFIX + userId, ttlMinutes, TimeUnit.MINUTES);
                return existingToken;
            }
        }
        return null;
    }

    // ======================== Internal Shared Operations ========================
    // 注: 以下方法的 read→modify 操作在 Redis 层面非原子性, 但竞态后果是良性的:
    // - setExpire 在 key 已删除时为 no-op (返回 false)
    // - 最坏情况是 getOrRefreshUserSession 在 key 被删除后重建了一个带 TTL 的短暂泄漏 key
    // - 这些操作的并发窗口极小, 且均有 TTL 自动清理保障

    private void invalidateSession(String idToTokenKey, String tokenToInfoPrefix) {
        Object existToken = redisUtil.get(idToTokenKey);
        if (existToken != null) {
            redisUtil.del(tokenToInfoPrefix + existToken);
            redisUtil.del(idToTokenKey);
        }
    }

    private String createSession(String idToTokenKey, String tokenToInfoPrefix, Object loginInfo, long ttlMinutes) {
        String token = IdUtil.fastSimpleUUID();
        redisUtil.set(idToTokenKey, token, ttlMinutes, TimeUnit.MINUTES);
        redisUtil.set(tokenToInfoPrefix + token, loginInfo, ttlMinutes, TimeUnit.MINUTES);
        return token;
    }

    private void refreshSessionTTL(String idToTokenKey, String tokenToInfoPrefix, long ttlMinutes) {
        Object tokenObj = redisUtil.get(idToTokenKey);
        if (tokenObj != null) {
            redisUtil.setExpire(idToTokenKey, ttlMinutes, TimeUnit.MINUTES);
            redisUtil.setExpire(tokenToInfoPrefix + tokenObj, ttlMinutes, TimeUnit.MINUTES);
        }
    }

    private void refreshLoginInfo(String idToTokenKey, String tokenToInfoPrefix, Object loginInfo) {
        Object existToken = redisUtil.get(idToTokenKey);
        if (existToken != null) {
            long remainingSeconds = redisUtil.getExpire(tokenToInfoPrefix + existToken);
            if (remainingSeconds > 0) {
                redisUtil.set(tokenToInfoPrefix + existToken, loginInfo, remainingSeconds);
            } else {
                redisUtil.set(tokenToInfoPrefix + existToken, loginInfo);
            }
        }
    }

}
