package cn.projectan.strix.model.enums.system;

import lombok.Getter;

/**
 * 聊天会话类型枚举
 *
 * @author ProjectAn
 * @since 2026/2/1 12:00
 */
@Getter
public enum ChatSessionTypeEnum {

    /**
     * 单聊
     */
    SINGLE("SINGLE", "单聊"),

    /**
     * 群聊
     */
    GROUP("GROUP", "群聊");

    private final String codeValue;
    private final String codeDesc;

    ChatSessionTypeEnum(String codeValue, String codeDesc) {
        this.codeValue = codeValue;
        this.codeDesc = codeDesc;
    }

    /**
     * 根据 codeValue 获取枚举
     *
     * @param codeValue 编码值
     * @return 枚举对象
     */
    public static ChatSessionTypeEnum parseFromCodeValue(String codeValue) {
        for (ChatSessionTypeEnum e : ChatSessionTypeEnum.values()) {
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
        ChatSessionTypeEnum enumItem = parseFromCodeValue(codeValue);
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
        for (ChatSessionTypeEnum e : ChatSessionTypeEnum.values()) {
            buffer.append(e.codeValue).append("--").append(e.getCodeDesc()).append(", ");
        }
        buffer.deleteCharAt(buffer.lastIndexOf(","));
        return buffer.toString().trim();
    }

}
