package cn.projectan.strix.utils;

import cn.hutool.core.lang.Snowflake;

/**
 * 雪花算法工具类
 *
 * @author ProjectAn
 * @date 2023/5/23 9:39
 */
public class SnowflakeUtil {

    private static final Snowflake SNOWFLAKE_OSS_FILE = new Snowflake(1, 1);
    private static final Snowflake SNOWFLAKE_SYSTEM_USER = new Snowflake(1, 2);

    /**
     * 生成OSS文件名
     *
     * @return OSS文件名
     */
    public static String nextOssFileName() {
        return String.valueOf(SNOWFLAKE_OSS_FILE.nextId());
    }

    /**
     * 生成系统用户ID
     *
     * @return 系统用户ID
     */
    public static String nextSystemUserId() {
        return String.valueOf(SNOWFLAKE_SYSTEM_USER.nextId());
    }

}
