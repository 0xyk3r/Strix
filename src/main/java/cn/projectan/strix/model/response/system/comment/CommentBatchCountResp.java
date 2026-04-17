package cn.projectan.strix.model.response.system.comment;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * 评论批量计数响应
 *
 * @author ProjectAn
 */
@Schema(description = "评论批量计数响应")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CommentBatchCountResp {

    @Schema(description = "评论数映射: bizId -> count")
    private Map<String, Long> counts;

}
