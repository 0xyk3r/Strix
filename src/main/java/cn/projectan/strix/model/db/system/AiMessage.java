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

/**
 * AI 对话消息
 *
 * @author ProjectAn
 * @since 2026-05-12
 */
@Getter
@Setter
@Accessors(chain = true)
@NoArgsConstructor
@AllArgsConstructor
@TableName("sys_ai_message")
public class AiMessage extends BaseModel<AiMessage> {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 关联会话 ID
     */
    private String sessionId;

    /**
     * 消息角色（user / assistant / system）
     */
    @TableField("`role`")
    private String role;

    /**
     * 消息文本内容
     */
    private String content;

    /**
     * 思考过程内容（qwen3 thinking 模式，仅 assistant 角色）
     */
    private String thinkingContent;

    /**
     * 多模态附件 JSON（[{type,url,name}]，视觉模型使用）
     */
    private String attachments;

    /**
     * 输入 Token 消耗
     */
    private Integer promptTokens;

    /**
     * 输出 Token 消耗
     */
    private Integer completionTokens;

    /**
     * 消息状态（0=生成中 1=完成 2=出错）
     *
     * @see cn.projectan.strix.model.dict.system.AiMessageStatus
     */
    @TableField("`status`")
    private Short status;

    /**
     * 错误信息（status=2 时填充）
     */
    private String errorMsg;

}
