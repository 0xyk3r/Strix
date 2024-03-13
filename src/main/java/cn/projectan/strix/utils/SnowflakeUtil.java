package cn.projectan.strix.utils;

import cn.hutool.core.lang.Snowflake;

/**
 * @author ProjectAn
 * @date 2023/5/23 9:39
 */
public class SnowflakeUtil {

    private static final Snowflake SNOWFLAKE_OSS_FILE = new Snowflake(1, 1);

    public static String nextOssFileName() {
        return String.valueOf(SNOWFLAKE_OSS_FILE.nextId());
    }

}
