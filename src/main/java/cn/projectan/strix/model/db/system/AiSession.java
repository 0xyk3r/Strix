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
 * AI 对话会话
 *
 * @author ProjectAn
 * @since 2026-05-12
 */
@Getter
@Setter
@Accessors(chain = true)
@NoArgsConstructor
@AllArgsConstructor
@TableName("sys_ai_session")
public class AiSession extends BaseModel<AiSession> {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 关联模型配置 ID
     */
    private String modelConfigId;

    /**
     * 创建该会话的管理员 ID
     */
    private String managerId;

    /**
     * 会话标题
     */
    @TableField("`title`")
    private String title;

    /**
     * 会话状态（0=活跃 1=已归档）
     */
    @TableField("`status`")
    private Short status;

    /**
     * 会话级系统提示词覆盖（N4）：非空则优先于模型配置的默认 systemPrompt，
     * 使同一模型在不同会话可用不同人设。空则回退模型配置默认值。
     */
    private String systemPrompt;

}
