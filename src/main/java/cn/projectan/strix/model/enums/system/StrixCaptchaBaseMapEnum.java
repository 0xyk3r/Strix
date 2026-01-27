package cn.projectan.strix.model.enums.system;

import lombok.Getter;

/**
 * Strix 验证码底图类型枚举
 *
 * @author ProjectAn
 * @since 2024/3/30 13:00
 */
@Getter
public enum StrixCaptchaBaseMapEnum {

    ORIGINAL("ORIGINAL", "滑动拼图底图"),
    SLIDING_BLOCK("SLIDING_BLOCK", "滑动拼图滑块底图"),
    PIC_CLICK("PIC_CLICK", "文字点选底图");

    private final String codeValue;
    private final String codeDesc;

    StrixCaptchaBaseMapEnum(String codeValue, String codeDesc) {
        this.codeValue = codeValue;
        this.codeDesc = codeDesc;
    }

    // 根据 codeValue 获取枚举
    public static StrixCaptchaBaseMapEnum parseFromCodeValue(String codeValue) {
        for (StrixCaptchaBaseMapEnum e : StrixCaptchaBaseMapEnum.values()) {
            if (e.codeValue.equals(codeValue)) {
                return e;
            }
        }
        return null;
    }

    // 根据 codeValue 获取描述
    public static String getCodeDescByCodeValue(String codeValue) {
        StrixCaptchaBaseMapEnum enumItem = parseFromCodeValue(codeValue);
        return enumItem == null ? "" : enumItem.getCodeDesc();
    }

    // 验证 codeValue 是否有效
    public static boolean validateCodeValue(String codeValue) {
        return parseFromCodeValue(codeValue) != null;
    }

    // 列出所有值字符串
    public static String getString() {
        StringBuilder buffer = new StringBuilder();
        for (StrixCaptchaBaseMapEnum e : StrixCaptchaBaseMapEnum.values()) {
            buffer.append(e.codeValue).append("--").append(e.getCodeDesc()).append(", ");
        }
        buffer.deleteCharAt(buffer.lastIndexOf(","));
        return buffer.toString().trim();
    }

}
