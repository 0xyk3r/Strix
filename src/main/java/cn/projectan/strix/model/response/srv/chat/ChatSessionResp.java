package cn.projectan.strix.model.response.srv.chat;

import cn.projectan.strix.model.db.system.ChatSession;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 聊天会话响应
 *
 * @author ProjectAn
 * @since 2026/2/1 12:00
 */
@Data
@Schema(description = "聊天 - 会话响应")
public class ChatSessionResp {

    @Schema(description = "会话 ID")
    private String sessionId;

    @Schema(description = "配置 ID")
    private String configId;

    @Schema(description = "配置 Key")
    private String configKey;

    @Schema(description = "配置名称")
    private String configName;

    @Schema(description = "会话类型（SINGLE/GROUP）")
    private String sessionType;

    @Schema(description = "业务类型")
    private String bizType;

    @Schema(description = "业务 ID")
    private String bizId;

    @Schema(description = "最后消息 ID")
    private String lastMsgId;

    @Schema(description = "最后消息时间")
    private LocalDateTime lastMsgTime;

    @Schema(description = "创建时间")
    private LocalDateTime createdTime;

    public ChatSessionResp() {
    }

    public ChatSessionResp(ChatSession session) {
        this.sessionId = session.getId();
        this.configId = session.getConfigId();
        this.sessionType = session.getType();
        this.bizType = session.getBizType();
        this.bizId = session.getBizId();
        this.lastMsgId = session.getLastMsgId();
        this.lastMsgTime = session.getLastMsgTime();
        this.createdTime = session.getCreatedTime();
    }

}
