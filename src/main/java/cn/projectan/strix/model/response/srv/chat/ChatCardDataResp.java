package cn.projectan.strix.model.response.srv.chat;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 聊天卡片数据响应
 *
 * @author ProjectAn
 * @since 2026/2/1 12:00
 */
@Data
@Schema(description = "聊天 - 卡片数据响应")
public class ChatCardDataResp {

    @Schema(description = "卡片类型")
    private String cardType;

    @Schema(description = "卡片数据 ID")
    private String cardDataId;

    @Schema(description = "卡片数据（业务方自定义结构）")
    private Object data;

}
