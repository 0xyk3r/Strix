package cn.projectan.strix.model.request.system.comment;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * 评论列表请求
 *
 * @author ProjectAn
 */
@Schema(description = "评论列表请求")
@Data
public class CommentListReq {

    @Schema(description = "业务类型")
    @NotEmpty(message = "业务类型不能为空")
    private String bizType;

    @Schema(description = "业务主键 ID")
    @NotEmpty(message = "业务主键不能为空")
    private String bizId;

    @Schema(description = "搜索关键词")
    @Size(max = 64)
    private String keyword;

}
