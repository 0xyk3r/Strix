package cn.projectan.strix.model.request.system.comment;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * 编辑评论请求
 *
 * @author ProjectAn
 */
@Schema(description = "编辑评论请求")
@Data
public class CommentUpdateReq {

    @Schema(description = "评论内容")
    @NotEmpty(message = "评论内容不能为空")
    @Size(max = 5000, message = "评论内容不能超过 5000 字")
    private String content;

    @Schema(description = "@提及的管理员 ID 列表")
    private List<String> mentionedIds;

    @Schema(description = "附件文件 ID 列表")
    private List<String> attachmentIds;

}
