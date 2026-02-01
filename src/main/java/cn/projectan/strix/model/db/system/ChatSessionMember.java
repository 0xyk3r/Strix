package cn.projectan.strix.model.db.system;

import cn.projectan.strix.model.db.base.BaseModel;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.io.Serial;
import java.time.LocalDateTime;

/**
 * <p>
 * Strix Chat 会话成员
 * </p>
 *
 * @author ProjectAn
 * @since 2026-02-01
 */
@Getter
@Setter
@Accessors(chain = true)
@NoArgsConstructor
@AllArgsConstructor
@TableName("sys_chat_session_member")
public class ChatSessionMember extends BaseModel<ChatSessionMember> {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 聊天会话 ID
     */
    private String sessionId;

    /**
     * 聊天成员用户 ID
     */
    private String userId;

    /**
     * 聊天成员身份 (OWNER / MEMBER)
     */
    @TableField("`role`")
    private String role;

    /**
     * 聊天成员最后已读消息 ID
     */
    private String lastReadId;

    /**
     * 聊天成员进入会话时间
     */
    private LocalDateTime joinTime;

    /**
     * 会话隐藏状态 (0=未隐藏 1=已隐藏，仅对一对一会话有效)
     */
    private Short hiddenStatus;

}
