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
 * Strix Chat 会话
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
@TableName("sys_chat_session")
public class ChatSession extends BaseModel<ChatSession> {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 聊天服务 ID
     */
    private String configId;

    /**
     * 聊天服务类型 (SINGLE一对一聊天 GROUP多人聊天)
     */
    @TableField("`type`")
    private String type;

    /**
     * 关联业务类型
     */
    private String bizType;

    /**
     * 关联业务 ID
     */
    private String bizId;

    /**
     * 最后消息 ID
     */
    private String lastMsgId;

    /**
     * 最后消息时间
     */
    private LocalDateTime lastMsgTime;

    /**
     * 群聊名称（仅 GROUP 类型）
     */
    private String groupName;

}
