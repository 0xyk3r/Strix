package cn.projectan.strix.model.constant.system;

/**
 * 登录会话 Redis Key 常量
 * <p>
 * 新架构: token→info (认证热路径) + registry (Hash, 会话注册表)
 *
 * @author ProjectAn
 * @since 2026/1/29 14:00
 */
public interface LoginRedisKeys {

    // ======================== Manager Session ========================

    /** 管理员 Token → LoginSystemManager (认证过滤器热路径) */
    String MANAGER_TOKEN_PREFIX = "strix:session:manager:token:";

    /** 管理员会话注册表 Hash: managerId → Hash<token, SessionMeta JSON> */
    String MANAGER_REGISTRY_PREFIX = "strix:session:manager:registry:";

    // ======================== User Session ========================

    /** 用户 Token → LoginSystemUser (认证过滤器热路径) */
    String USER_TOKEN_PREFIX = "strix:session:user:token:";

    /** 用户会话注册表 Hash: userId → Hash<token, SessionMeta JSON> */
    String USER_REGISTRY_PREFIX = "strix:session:user:registry:";
}
