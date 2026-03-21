package cn.projectan.strix.model.constant.system;

/**
 * Redis Key 常量
 * <br>
 * 命名规范: Key数据类型_具体业务名[_Key标识类型] <br>
 * Key数据类型: STR | LIST | SET | ZSET | HASH <br>
 * Key标识类型: PREFIX | SUFFIX
 *
 * @author ProjectAn
 * @since 2024/4/17 下午3:14
 */
public interface StrixRedisKeyConst {

    /**
     * 热度工具数据
     */
    String HASH_POPULARITY_DATA_PREFIX = "strix:popularity:data:";

    /**
     * 临时 URL - 公开
     */
    String STR_TEMP_URL_PUBLIC_PREFIX = "strix:util:temp-url:public::";

    /**
     * 临时 URL - 私有
     */
    String STR_TEMP_URL_PRIVATE_PREFIX = "strix:util:temp-url:private::";

    /**
     * 数据 ID 映射器
     */
    String HASH_NAME_FETCHER_PREFIX = "strix:name-fetcher::";

    /**
     * 数据操作人信息映射器
     */
    String HASH_OPERATOR_INFO_PREFIX = "strix:operator-info::";

    /**
     * 登录失败次数计数器（按 IP）
     */
    String STR_LOGIN_FAILURE_IP_PREFIX = "strix:login:failure:ip:";

}
