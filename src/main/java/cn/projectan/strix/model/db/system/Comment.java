package cn.projectan.strix.model.db.system;

import cn.projectan.strix.model.db.base.BaseModel;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.io.Serial;
import java.util.List;

/**
 * 通用评论
 *
 * @author ProjectAn
 */
@Getter
@Setter
@Accessors(chain = true)
@NoArgsConstructor
@AllArgsConstructor
@TableName(value = "sys_comment", autoResultMap = true)
public class Comment extends BaseModel<Comment> {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 业务类型（实体类型标识，如 SystemUser、Dict 等）
     */
    private String bizType;

    /**
     * 业务主键 ID
     */
    private String bizId;

    /**
     * 评论内容（支持 Markdown 格式和 @提及）
     */
    private String content;

    /**
     * @提及的管理员 ID 列表 (JSON)
     */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<String> mentionedIds;

    /**
     * 附件文件 ID 列表 (JSON)
     */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<String> attachmentIds;

    /**
     * 置顶标记 (0=否 1=是)
     *
     * @see cn.projectan.strix.model.dict.common.CommonFlag
     */
    private Short pinned;

}
