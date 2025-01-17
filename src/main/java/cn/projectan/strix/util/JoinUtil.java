package cn.projectan.strix.util;

import java.util.List;

/**
 * 字符串拼接工具类
 *
 * @author ProjectAn
 * @since 2025-01-12 22:04:01
 */
public class JoinUtil {

    /**
     * 拼接字符串
     *
     * @param list      字符串列表
     * @param delimiter 分隔符
     * @return 拼接后的字符串
     */
    public static String joinString(CharSequence delimiter, List<String> list) {
        return String.join(delimiter, list);
    }

    /**
     * 拼接任意类型
     *
     * @param list      列表
     * @param delimiter 分隔符
     * @return 拼接后的字符串
     */
    public static String joinAny(CharSequence delimiter, List<?> list) {
        return String.join(delimiter, list.stream().map(String::valueOf).toArray(String[]::new));
    }

}
