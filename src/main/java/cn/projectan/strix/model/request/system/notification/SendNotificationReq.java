package cn.projectan.strix.model.request.system.notification;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;
import cn.projectan.strix.model.annotation.FormSchema;

/**
 * 发送通知请求
 *
 * @author ProjectAn
 * @since 2026-03-26
 */
@Schema(description = "发送通知请求")
@FormSchema
@Data
public class SendNotificationReq {

    @Schema(description = "通知标题")
    @NotBlank(message = "通知标题不能为空")
    @Size(max = 200, message = "通知标题不能超过200字")
    private String title;

    @Schema(description = "通知内容")
    @NotBlank(message = "通知内容不能为空")
    private String content;

    @Schema(description = "发送方式: BROADCAST(系统广播) / TARGETED(定向通知)")
    @NotBlank(message = "发送方式不能为空")
    private String sendMode;

    @Schema(description = "接收人 ID 列表 (定向通知时必填)")
    private List<String> receiverIds;

    @Schema(description = "跳转类型: NONE / PAGE / URL, 默认 NONE")
    private String jumpType;

    @Schema(description = "跳转目标 (路由名称或 URL)")
    private String jumpTarget;

    @Schema(description = "跳转参数 (JSON)")
    private String jumpParams;
}
