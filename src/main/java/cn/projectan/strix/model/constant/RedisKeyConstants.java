package cn.projectan.strix.model.constant;

/**
 * Redis Key 常量
 * <br>
 * 命名规范: Key数据类型_具体业务名[_Key标识类型] <br>
 * Key数据类型: STR | LIST | SET | ZSET | HASH <br>
 * Key标识类型: PREFIX | SUFFIX
 *
 * @author ProjectAn
 * @date 2024/4/17 下午3:14
 */
public class RedisKeyConstants {

    public static final String HASH_POPULARITY_DATA_PREFIX = "strix:popularity:data:";

    public static final String STR_TEMP_URL_PUBLIC_PREFIX = "strix:util:temp-url:public::";

    public static final String STR_TEMP_URL_PRIVATE_PREFIX = "strix:util:temp-url:private::";

}
