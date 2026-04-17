package cn.projectan.strix.model.request.system.comment;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

/**
 * 评论反应请求（切换 Emoji 反应）
 *
 * @author ProjectAn
 */
@Schema(description = "评论反应请求")
@Data
public class CommentReactionReq {

    @Schema(description = "表情标识", example = "thumbsup")
    @NotEmpty(message = "表情标识不能为空")
    private String emoji;

}
