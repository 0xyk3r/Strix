package cn.projectan.strix.model.db.system;

import cn.projectan.strix.model.db.base.BaseModel;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.io.Serial;

/**
 * 评论快捷反应
 *
 * @author ProjectAn
 */
@Getter
@Setter
@Accessors(chain = true)
@NoArgsConstructor
@AllArgsConstructor
@TableName("sys_comment_reaction")
public class CommentReaction extends BaseModel<CommentReaction> {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 评论 ID
     */
    private String commentId;

    /**
     * 表情标识 (如 thumbsup, heart, check 等)
     */
    private String emoji;

}
