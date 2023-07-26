package cn.projectan.strix.utils;

/**
 * @author 安炯奕
 * @date 2021/6/11 18:14
 */
public class NumUtils {

    /**
     * 判断 Integer 是否非空且大于0
     *
     * @param i 需要判断的值
     * @return 是否非空且大于0
     */
    public static boolean isPositiveNumber(Integer i) {
        return i != null && i > 0;
    }

    /**
     * 判断 Integer 是否非空且大于等于0
     *
     * @param i 需要判断的值
     * @return 是否非空且大于等于0
     */
    public static boolean isNonNegativeNumber(Integer i) {
        return i != null && i >= 0;
    }

}
