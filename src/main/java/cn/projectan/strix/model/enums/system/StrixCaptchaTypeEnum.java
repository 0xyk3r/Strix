package cn.projectan.strix.model.enums.system;

import lombok.Getter;

/**
 * Strix 验证码类型枚举
 *
 * @author ProjectAn
 * @since 2024/3/30 13:00
 */
@Getter
public enum StrixCaptchaTypeEnum {

    /**
     * 滑块拼图
     */
    BLOCK_PUZZLE("blockPuzzle", "滑块拼图");

    private final String codeValue;
    private final String codeDesc;

    StrixCaptchaTypeEnum(String codeValue, String codeDesc) {
        this.codeValue = codeValue;
        this.codeDesc = codeDesc;
    }

    /**
     * 根据 codeValue 获取枚举
     *
     * @param codeValue 编码值
     * @return 枚举对象
     */
    public static StrixCaptchaTypeEnum parseFromCodeValue(String codeValue) {
        for (StrixCaptchaTypeEnum e : StrixCaptchaTypeEnum.values()) {
            if (e.codeValue.equals(codeValue)) {
                return e;
            }
        }
        return null;
    }

    /**
     * 根据 codeValue 获取描述
     *
     * @param codeValue 编码值
     * @return 描述
     */
    public static String getCodeDescByCodeValue(String codeValue) {
        StrixCaptchaTypeEnum enumItem = parseFromCodeValue(codeValue);
        return enumItem == null ? "" : enumItem.getCodeDesc();
    }

    /**
     * 验证 codeValue 是否有效
     *
     * @param codeValue 编码值
     * @return 是否有效
     */
    public static boolean validateCodeValue(String codeValue) {
        return parseFromCodeValue(codeValue) != null;
    }

    /**
     * 获取所有枚举的字符串表示
     *
     * @return 字符串表示
     */
    public static String getString() {
        StringBuilder buffer = new StringBuilder();
        for (StrixCaptchaTypeEnum e : StrixCaptchaTypeEnum.values()) {
            buffer.append(e.codeValue).append("--").append(e.getCodeDesc()).append(", ");
        }
        buffer.deleteCharAt(buffer.lastIndexOf(","));
        return buffer.toString().trim();
    }

}
