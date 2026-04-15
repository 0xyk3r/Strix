package cn.projectan.strix.model.request.system.announcement;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 发布公告请求
 *
 * @author ProjectAn
 * @since 2026-04-18
 */
@Schema(description = "发布公告请求")
@Data
public class PublishAnnouncementReq {

    @Schema(description = "公告标题")
    @NotBlank(message = "公告标题不能为空")
    @Size(max = 200, message = "公告标题不能超过200字")
    private String title;

    @Schema(description = "公告内容")
    @NotBlank(message = "公告内容不能为空")
    private String content;

    @Schema(description = "级别: INFO / WARNING / URGENT")
    @NotBlank(message = "公告级别不能为空")
    private String level;

    @Schema(description = "展示方式: BANNER / MODAL")
    @NotBlank(message = "展示方式不能为空")
    private String displayType;

    @Schema(description = "生效时间 (null = 立即生效)")
    private LocalDateTime startTime;

    @Schema(description = "失效时间 (null = 不自动失效)")
    private LocalDateTime endTime;
}
