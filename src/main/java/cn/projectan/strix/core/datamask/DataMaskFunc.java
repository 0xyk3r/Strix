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
     * 保留前n位，后面用脱敏字符代替
     */
    KEEP_FRONT((data, maskChar, n1, n2) -> {
        if (!StringUtils.hasText(data)) {
            return null;
        }
        if (data.length() <= n1) {
            return String.valueOf(maskChar).repeat(6);
        }
        return data.substring(0, n1) + String.valueOf(maskChar).repeat(data.length() - n1);
    }),
    /**
     * 保留后n位，前面用脱敏字符代替
     */
    KEEP_BACK((data, maskChar, n1, n2) -> {
        if (!StringUtils.hasText(data)) {
            return null;
        }
        if (data.length() <= n1) {
            return String.valueOf(maskChar).repeat(6);
        }
        return String.valueOf(maskChar).repeat(data.length() - n1) + data.substring(data.length() - n1);
    }),
    /**
     * 保留前n1位和后n2位，中间用脱敏字符代替
     */
    KEEP_SIDE((data, maskChar, n1, n2) -> {
        if (!StringUtils.hasText(data)) {
            return null;
        }
        if (data.length() <= n1 + n2) {
            return String.valueOf(maskChar).repeat(6);
        }
        return data.substring(0, n1) + String.valueOf(maskChar).repeat(data.length() - n1 - n2) + data.substring(data.length() - n2);
    }),
    /**
     * 不保留任何位，全部用脱敏字符代替
     */
    KEEP_NONE((data, maskChar, n1, n2) -> {
        if (!StringUtils.hasText(data)) {
            return null;
        }
        return String.valueOf(maskChar).repeat(data.length());
    });


    private final DataMaskOperation operation;

    DataMaskFunc(DataMaskOperation operation) {
        this.operation = operation;
    }

    public DataMaskOperation operation() {
        return this.operation;
    }

}
