package cn.projectan.strix.model.enums;

import lombok.Getter;

/**
 * Strix 验证码类型枚举
 *
 * @author ProjectAn
 * @date 2024/3/30 13:00
 */
@Getter
public enum CaptchaTypeEnum {

    /**
     * 滑块拼图
     */
    BLOCKPUZZLE("blockPuzzle", "滑块拼图"),
    /**
     * 文字点选
     */
    CLICKWORD("clickWord", "文字点选"),
    /**
     * 默认
     */
    DEFAULT("default", "默认");

    private final String codeValue;
    private final String codeDesc;

    CaptchaTypeEnum(String codeValue, String codeDesc) {
        this.codeValue = codeValue;
        this.codeDesc = codeDesc;
    }

    // 根据 codeValue 获取枚举
    public static CaptchaTypeEnum parseFromCodeValue(String codeValue) {
        for (CaptchaTypeEnum e : CaptchaTypeEnum.values()) {
            if (e.codeValue.equals(codeValue)) {
                return e;
            }
        }
        return null;
    }

    // 根据 codeValue 获取描述
    public static String getCodeDescByCodeValue(String codeValue) {
        CaptchaTypeEnum enumItem = parseFromCodeValue(codeValue);
        return enumItem == null ? "" : enumItem.getCodeDesc();
    }

    // 验证 codeValue 是否有效
    public static boolean validateCodeValue(String codeValue) {
        return parseFromCodeValue(codeValue) != null;
    }

    // 列出所有值字符串
    public static String getString() {
        StringBuilder buffer = new StringBuilder();
        for (CaptchaTypeEnum e : CaptchaTypeEnum.values()) {
            buffer.append(e.codeValue).append("--").append(e.getCodeDesc()).append(", ");
        }
        buffer.deleteCharAt(buffer.lastIndexOf(","));
        return buffer.toString().trim();
    }

}
