package cn.projectan.strix.model.enums.system;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

/**
 * 聊天 WebSocket 通知类型枚举
 *
 * @author ProjectAn
 * @since 2026/2/1 12:00
 */
@Getter
@Schema(description = "聊天 WebSocket 通知类型")
public enum ChatWebSocketNotifyTypeEnum {

    /**
     * 新消息通知
     */
    NEW_MSG("NEW_MSG", "新消息通知"),

    /**
     * 会话更新通知
     */
    SESSION_UPDATE("SESSION_UPDATE", "会话更新通知");

    private final String codeValue;
    private final String codeDesc;

    ChatWebSocketNotifyTypeEnum(String codeValue, String codeDesc) {
        this.codeValue = codeValue;
        this.codeDesc = codeDesc;
    }

    /**
     * 根据 codeValue 获取枚举
     *
     * @param codeValue 编码值
     * @return 枚举对象
     */
    public static ChatWebSocketNotifyTypeEnum parseFromCodeValue(String codeValue) {
        for (ChatWebSocketNotifyTypeEnum e : ChatWebSocketNotifyTypeEnum.values()) {
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
        ChatWebSocketNotifyTypeEnum enumItem = parseFromCodeValue(codeValue);
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
        for (ChatWebSocketNotifyTypeEnum e : ChatWebSocketNotifyTypeEnum.values()) {
            buffer.append(e.codeValue).append("--").append(e.getCodeDesc()).append(", ");
        }
        buffer.deleteCharAt(buffer.lastIndexOf(","));
        return buffer.toString().trim();
    }

}
