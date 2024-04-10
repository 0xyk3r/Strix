package cn.projectan.strix.utils;

import cn.projectan.strix.model.enums.NumCategory;

/**
 * Number 工具类
 *
 * @author ProjectAn
 * @date 2021/6/11 18:14
 */
public class NumUtil {

    /**
     * 判断数字是否属于指定的分类
     *
     * @param i        数字
     * @param category 分类
     * @param <T>      数字类型
     * @return 是否属于指定的分类
     */
    public static <T extends Number> boolean checkCategory(T i, NumCategory category) {
        if (i == null) {
            return false;
        }
        double value = i.doubleValue();
        return switch (category) {
            case POSITIVE -> value > 0;
            case NON_NEGATIVE -> value >= 0;
            case NEGATIVE -> value < 0;
            case NON_POSITIVE -> value <= 0;
            case ZERO -> value == 0;
            case NOT_ZERO -> value != 0;
        };
    }

}
