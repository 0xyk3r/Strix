package cn.projectan.strix.core.datamask;

import org.springframework.util.StringUtils;

/**
 * 数据脱敏函数
 *
 * @author ProjectAn
 * @since 2023/2/22 14:30
 */
public enum DataMaskFunc {

    /**
     * 保留前n位，后面用*代替
     */
    KEEP_FRONT((data, maskChar, n1, n2) -> {
        if (!StringUtils.hasText(data)) {
            return null;
        }
        if (data.length() <= n1) {
            return "******";
        }
        return data.replaceAll("(?<=\\w{" + n1 + "})\\w", "*");
    }),
    /**
     * 保留后n位，前面用*代替
     */
    KEEP_BACK((data, maskChar, n1, n2) -> {
        if (!StringUtils.hasText(data)) {
            return null;
        }
        if (data.length() <= n1) {
            return "******";
        }
        return data.replaceAll("(?<=\\w{" + n1 + "})\\w", "*");
    }),
    /**
     * 保留前后各n位，中间用*代替
     */
    KEEP_SIDE((data, maskChar, n1, n2) -> {
        if (!StringUtils.hasText(data)) {
            return null;
        }
        if (data.length() <= n1 + n2) {
            return data;
        }
        return data.replaceAll("(?<=\\w{" + n1 + "})\\w(?=\\w{" + n2 + "})", "*");
    }),
    /**
     * 不保留任何位，全部用*代替
     */
    KEEP_NONE((data, maskChar, n1, n2) -> {
        if (!StringUtils.hasText(data)) {
            return null;
        }
        return data.replaceAll("\\w", "*");
    });


    private final DataMaskOperation operation;

    DataMaskFunc(DataMaskOperation operation) {
        this.operation = operation;
    }

    public DataMaskOperation operation() {
        return this.operation;
    }

}
