package cn.projectan.strix.service.system;

import cn.hutool.core.util.IdUtil;
import cn.projectan.strix.core.ss.details.LoginSystemManager;
import cn.projectan.strix.core.ss.details.LoginSystemUser;
import cn.projectan.strix.model.constant.system.LoginRedisKeys;
import cn.projectan.strix.model.response.system.monitor.session.SessionMeta;
import cn.projectan.strix.util.common.RedisUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * Token 会话管理服务
 * <p>
 * 新架构：Hash-based session registry + token→info 热路径
 * <ul>
 *   <li>token→info: 认证过滤器的 O(1) 查询路径</li>
 *   <li>registry: Hash&lt;token, SessionMeta&gt;, 按用户 ID 聚合所有会话, 支持多端/枚举/管理</li>
 * </ul>
 *
 * @author ProjectAn
 * @since 2026-03-22
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TokenSessionService {

    private final RedisUtil redisUtil;
    private final ObjectMapper objectMapper;

    // ======================== Manager Session ========================

    /**
     * 创建管理员会话, 返回新 token
     *
     * @param managerId   管理员 ID
     * @param loginInfo   登录信息 (存入 token→info 键)
     * @param ttlMinutes  TTL (分钟)
     * @param sessionMeta 会话元数据 (存入 registry Hash)
     * @return 新 token
     */
    public String createManagerSession(String managerId, LoginSystemManager loginInfo,
                                       long ttlMinutes, SessionMeta sessionMeta) {
        return createSession(
                LoginRedisKeys.MANAGER_TOKEN_PREFIX,
                LoginRedisKeys.MANAGER_REGISTRY_PREFIX + managerId,
                loginInfo, ttlMinutes, sessionMeta
        );
    }

    /**
     * 使管理员所有会话失效 (踢出所有设备)
     */
    public void invalidateManagerSession(String managerId) {
        invalidateAllSessions(
                LoginRedisKeys.MANAGER_TOKEN_PREFIX,
                LoginRedisKeys.MANAGER_REGISTRY_PREFIX + managerId
        );
    }

    /**
     * 管理员登出 (根据 token 登出单个会话)
     */
    public void logoutManager(String token) {
        Object loginInfo = redisUtil.get(LoginRedisKeys.MANAGER_TOKEN_PREFIX + token);
        redisUtil.del(LoginRedisKeys.MANAGER_TOKEN_PREFIX + token);

        if (loginInfo instanceof LoginSystemManager lsm && lsm.getSystemManager() != null) {
            String managerId = lsm.getSystemManager().getId();
            if (managerId != null) {
                redisUtil.hDel(LoginRedisKeys.MANAGER_REGISTRY_PREFIX + managerId, token);
            }
        }
    }

    /**
     * 踢出管理员的指定会话
     */
    public void kickManagerSession(String managerId, String token) {
        redisUtil.del(LoginRedisKeys.MANAGER_TOKEN_PREFIX + token);
        redisUtil.hDel(LoginRedisKeys.MANAGER_REGISTRY_PREFIX + managerId, token);
    }

    /**
     * 获取管理员所有会话的元数据
     *
     * @return Map&lt;token, SessionMeta&gt;
     */
    public Map<String, SessionMeta> getManagerSessions(String managerId) {
        return getSessions(LoginRedisKeys.MANAGER_REGISTRY_PREFIX + managerId);
    }

    /**
     * 根据管理员 ID 获取其当前登录信息 (返回任意一个有效 session 的 LoginInfo)
     */
    public LoginSystemManager getManagerLoginInfoById(String managerId) {
        Map<Object, Object> entries = redisUtil.hEntries(LoginRedisKeys.MANAGER_REGISTRY_PREFIX + managerId);
        if (entries == null || entries.isEmpty()) {
            return null;
        }
        for (Object tokenObj : entries.keySet()) {
            if (tokenObj instanceof String token) {
                Object loginInfo = redisUtil.get(LoginRedisKeys.MANAGER_TOKEN_PREFIX + token);
                if (loginInfo instanceof LoginSystemManager lsm) {
                    return lsm;
                }
            }
        }
        return null;
    }

    /**
     * 刷新管理员所有会话的 TTL
     */
    public void refreshManagerSessionTTL(String managerId, long ttlMinutes) {
        refreshAllSessionTTL(
                LoginRedisKeys.MANAGER_TOKEN_PREFIX,
                LoginRedisKeys.MANAGER_REGISTRY_PREFIX + managerId,
                ttlMinutes
        );
    }

    /**
     * 刷新管理员登录信息 (所有 token→info 键更新, 保持现有 TTL)
     */
    public void refreshManagerLoginInfo(String managerId, LoginSystemManager loginInfo) {
        refreshLoginInfo(
                LoginRedisKeys.MANAGER_TOKEN_PREFIX,
                LoginRedisKeys.MANAGER_REGISTRY_PREFIX + managerId,
                loginInfo
        );
    }

    /**
     * 更新会话的最后活跃时间
     */
    public void updateManagerLastActiveTime(String managerId, String token) {
        String registryKey = LoginRedisKeys.MANAGER_REGISTRY_PREFIX + managerId;
        Object metaObj = redisUtil.hGet(registryKey, token);
        if (metaObj == null) {
            return;
        }
        try {
            SessionMeta meta = deserializeMeta(metaObj);
            if (meta != null) {
                meta.setLastActiveTime(LocalDateTime.now());
                redisUtil.hSet(registryKey, token, serializeMeta(meta));
            }
        } catch (Exception e) {
            log.warn("更新会话最后活跃时间失败: managerId={}, token={}", managerId, token, e);
        }
    }

    /**
     * 获取所有在线管理员 ID (通过 SCAN registry 前缀)
     */
    public Set<String> getOnlineManagerIds() {
        Set<String> keys = redisUtil.scan(LoginRedisKeys.MANAGER_REGISTRY_PREFIX + "*");
        Set<String> managerIds = new HashSet<>();
        for (String key : keys) {
            String managerId = key.substring(LoginRedisKeys.MANAGER_REGISTRY_PREFIX.length());
            managerIds.add(managerId);
        }
        return managerIds;
    }

    /**
     * 强制执行最大会话数限制 — 踢出最旧的超额会话
     *
     * @param managerId   管理员 ID
     * @param maxSessions 最大允许会话数 (0=不限制)
     */
    public void enforceMaxSessions(String managerId, int maxSessions) {
        if (maxSessions <= 0) {
            return;
        }
        Map<String, SessionMeta> sessions = getManagerSessions(managerId);
        if (sessions.size() <= maxSessions) {
            return;
        }

        List<Map.Entry<String, SessionMeta>> sorted = sessions.entrySet().stream()
                .sorted(Comparator.comparing(
                        e -> e.getValue().getLoginTime() != null ? e.getValue().getLoginTime() : LocalDateTime.MIN))
                .toList();

        int toKick = sorted.size() - maxSessions;
        for (int i = 0; i < toKick; i++) {
            String token = sorted.get(i).getKey();
            kickManagerSession(managerId, token);
            log.info("自动踢出管理员 {} 的超额会话: token={}", managerId, token.substring(0, Math.min(4, token.length())) + "****");
        }
    }

    // ======================== User Session ========================

    /**
     * 使用户所有会话失效
     */
    public void invalidateUserSession(String userId) {
        invalidateAllSessions(
                LoginRedisKeys.USER_TOKEN_PREFIX,
                LoginRedisKeys.USER_REGISTRY_PREFIX + userId
        );
    }

    /**
     * 创建用户会话, 返回新 token
     */
    public String createUserSession(String userId, Object loginInfo,
                                    long ttlMinutes, SessionMeta sessionMeta) {
        return createSession(
                LoginRedisKeys.USER_TOKEN_PREFIX,
                LoginRedisKeys.USER_REGISTRY_PREFIX + userId,
                loginInfo, ttlMinutes, sessionMeta
        );
    }

    /**
     * 获取或刷新用户 session: 如果已有有效 token 则刷新 TTL 并返回, 否则返回 null
     */
    public String getOrRefreshUserSession(String userId, LoginSystemUser loginInfo, long ttlMinutes) {
        Map<Object, Object> entries = redisUtil.hEntries(LoginRedisKeys.USER_REGISTRY_PREFIX + userId);
        if (entries == null || entries.isEmpty()) {
            return null;
        }
        for (Object tokenObj : entries.keySet()) {
            if (tokenObj instanceof String existingToken) {
                Object cachedObj = redisUtil.get(LoginRedisKeys.USER_TOKEN_PREFIX + existingToken);
                if (cachedObj instanceof LoginSystemUser) {
                    redisUtil.set(LoginRedisKeys.USER_TOKEN_PREFIX + existingToken, loginInfo, ttlMinutes, TimeUnit.MINUTES);
                    redisUtil.setExpire(LoginRedisKeys.USER_REGISTRY_PREFIX + userId, ttlMinutes, TimeUnit.MINUTES);
                    return existingToken;
                }
            }
        }
        return null;
    }

    // ======================== Internal Shared Operations ========================

    private String createSession(String tokenPrefix, String registryKey,
                                 Object loginInfo, long ttlMinutes, SessionMeta sessionMeta) {
        String token = IdUtil.fastSimpleUUID();
        redisUtil.set(tokenPrefix + token, loginInfo, ttlMinutes, TimeUnit.MINUTES);
        redisUtil.hSet(registryKey, token, serializeMeta(sessionMeta));
        redisUtil.setExpire(registryKey, ttlMinutes, TimeUnit.MINUTES);
        return token;
    }

    private void invalidateAllSessions(String tokenPrefix, String registryKey) {
        Map<Object, Object> entries = redisUtil.hEntries(registryKey);
        if (entries != null) {
            for (Object tokenObj : entries.keySet()) {
                if (tokenObj instanceof String token) {
                    redisUtil.del(tokenPrefix + token);
                }
            }
        }
        redisUtil.del(registryKey);
    }

    private void refreshAllSessionTTL(String tokenPrefix, String registryKey, long ttlMinutes) {
        Map<Object, Object> entries = redisUtil.hEntries(registryKey);
        if (entries != null) {
            for (Object tokenObj : entries.keySet()) {
                if (tokenObj instanceof String token) {
                    redisUtil.setExpire(tokenPrefix + token, ttlMinutes, TimeUnit.MINUTES);
                }
            }
            redisUtil.setExpire(registryKey, ttlMinutes, TimeUnit.MINUTES);
        }
    }

    private void refreshLoginInfo(String tokenPrefix, String registryKey, Object loginInfo) {
        Map<Object, Object> entries = redisUtil.hEntries(registryKey);
        if (entries != null) {
            for (Object tokenObj : entries.keySet()) {
                if (tokenObj instanceof String token) {
                    long remainingSeconds = redisUtil.getExpire(tokenPrefix + token);
                    if (remainingSeconds > 0) {
                        redisUtil.set(tokenPrefix + token, loginInfo, remainingSeconds);
                    } else {
                        redisUtil.set(tokenPrefix + token, loginInfo);
                    }
                }
            }
        }
    }

    private Map<String, SessionMeta> getSessions(String registryKey) {
        Map<Object, Object> entries = redisUtil.hEntries(registryKey);
        Map<String, SessionMeta> result = new LinkedHashMap<>();
        if (entries == null) {
            return result;
        }
        for (Map.Entry<Object, Object> entry : entries.entrySet()) {
            if (entry.getKey() instanceof String token) {
                SessionMeta meta = deserializeMeta(entry.getValue());
                if (meta != null) {
                    result.put(token, meta);
                }
            }
        }
        return result;
    }

    private String serializeMeta(SessionMeta meta) {
        try {
            return objectMapper.writeValueAsString(meta);
        } catch (Exception e) {
            log.error("序列化 SessionMeta 失败", e);
            return "{}";
        }
    }

    private SessionMeta deserializeMeta(Object value) {
        try {
            String json = value instanceof String s ? s : value.toString();
            return objectMapper.readValue(json, SessionMeta.class);
        } catch (Exception e) {
            log.warn("反序列化 SessionMeta 失败: {}", value, e);
            return null;
        }
    }
}
