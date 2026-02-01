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
 * <p>
 * Strix Chat 配置
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
@TableName("sys_chat_config")
public class ChatConfig extends BaseModel<ChatConfig> {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 聊天服务 Key
     */
    @TableField("`key`")
    private String key;

    /**
     * 聊天服务名称
     */
    @TableField("`name`")
    private String name;

    /**
     * 聊天服务类型 (SINGLE一对一聊天 GROUP多人聊天)
     */
    private String sessionType;

    /**
     * 聊天服务备注
     */
    private String remark;

}
