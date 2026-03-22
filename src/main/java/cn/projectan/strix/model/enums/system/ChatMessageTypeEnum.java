package cn.projectan.strix.model.enums.system;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

/**
 * 聊天消息类型枚举
 *
 * @author ProjectAn
 * @since 2026/2/1 12:00
 */
@Getter
@Schema(description = "聊天消息类型")
public enum ChatMessageTypeEnum {

    /**
     * 文本消息
     */
    TEXT("TEXT", "文本消息"),

    /**
     * 图片消息
     */
    IMAGE("IMAGE", "图片消息"),

    /**
     * 卡片消息
     */
    CARD("CARD", "卡片消息");

    private final String codeValue;
    private final String codeDesc;

    ChatMessageTypeEnum(String codeValue, String codeDesc) {
        this.codeValue = codeValue;
        this.codeDesc = codeDesc;
    }

    /**
     * 根据 codeValue 获取枚举
     *
     * @param codeValue 编码值
     * @return 枚举对象
     */
    public static ChatMessageTypeEnum parseFromCodeValue(String codeValue) {
        for (ChatMessageTypeEnum e : ChatMessageTypeEnum.values()) {
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
        ChatMessageTypeEnum enumItem = parseFromCodeValue(codeValue);
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
        for (ChatMessageTypeEnum e : ChatMessageTypeEnum.values()) {
            buffer.append(e.codeValue).append("--").append(e.getCodeDesc()).append(", ");
        }
        buffer.deleteCharAt(buffer.lastIndexOf(","));
        return buffer.toString().trim();
    }

}
