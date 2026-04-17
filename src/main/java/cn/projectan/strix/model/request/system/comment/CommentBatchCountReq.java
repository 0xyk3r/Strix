package cn.projectan.strix.model.request.system.comment;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

/**
 * 批量获取评论计数请求
 *
 * @author ProjectAn
 */
@Schema(description = "批量获取评论计数请求")
@Data
public class CommentBatchCountReq {

    @Schema(description = "业务类型")
    @NotEmpty(message = "业务类型不能为空")
    private String bizType;

    @Schema(description = "业务主键 ID 列表")
    @NotEmpty(message = "业务主键列表不能为空")
    private List<String> bizIds;

}
