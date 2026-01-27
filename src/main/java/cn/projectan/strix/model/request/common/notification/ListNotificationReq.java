package cn.projectan.strix.model.request.common.notification;

import cn.projectan.strix.model.db.NotificationReceiver;
import cn.projectan.strix.model.request.base.BasePageReq;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 查询通知列表请求
 *
 * @author ProjectAn
 * @since 2026/1/13 16:40
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "查询通知列表请求")
public class ListNotificationReq extends BasePageReq<NotificationReceiver> {

    @Schema(description = "已读状态 (0未读 1已读，null查询全部)", example = "0")
    private Short readStatus;

    @Schema(description = "有效状态 (1有效 2失效，null查询全部)", example = "1")
    private Short validStatus;

}
