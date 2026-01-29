package cn.projectan.strix.model.constant.system;

/**
 * @author ProjectAn
 * @since 2026/1/29 14:00
 */
public interface LoginRedisKeys {

    String LOGIN_MANAGER_ID_TO_TOKEN_PREFIX = "strix:system:manager:login_token:login:id_";

    String LOGIN_MANAGER_TOKEN_TO_USER_INFO_PREFIX = "strix:system:manager:login_token:token:";

    String LOGIN_USER_ID_TO_TOKEN_PREFIX = "strix:system:user:login_token:login:id_";

    String LOGIN_USER_TOKEN_TO_USER_INFO_PREFIX = "strix:system:user:login_token:token:";

}
