package cn.projectan.strix.model.response.system.comment;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * 评论列表响应
 *
 * @author ProjectAn
 */
@Schema(description = "评论列表响应")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CommentListResp {

    @Schema(description = "评论列表")
    private List<CommentResp> items;

    @Schema(description = "总评论数")
    private long total;

}
