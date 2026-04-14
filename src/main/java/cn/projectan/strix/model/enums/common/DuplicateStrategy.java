package cn.projectan.strix.model.enums.common;

/**
 * 批量导入重复数据处理策略
 *
 * @author ProjectAn
 */
public enum DuplicateStrategy {

    /**
     * 跳过重复数据
     */
    SKIP,

    /**
     * 覆盖更新已有数据
     */
    UPSERT;

    public static DuplicateStrategy fromString(String value) {
        if (value == null) return SKIP;
        try {
            return valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            return SKIP;
        }
    }

}
