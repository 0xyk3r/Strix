package cn.projectan.strix.model.request.srv.chat;

import cn.projectan.strix.model.request.base.BasePageReq;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 会话列表请求
 *
 * @author ProjectAn
 * @since 2026/2/1 12:00
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "聊天 - 会话列表请求")
public class SessionListReq extends BasePageReq<Object> {

    @Schema(description = "配置 Key（可选筛选）", example = "CUSTOMER_SERVICE")
    private String configKey;

    @Schema(description = "业务类型（可选筛选）", example = "ORDER")
    private String bizType;

}
