package cn.projectan.strix.model.request.system.module.sms;

import cn.projectan.strix.model.db.system.SmsLog;
import cn.projectan.strix.model.request.base.BasePageReq;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * @author ProjectAn
 * @since 2023/5/22 13:24
 */
@Schema(description = "短信日志列表请求")
@Data
public class SmsLogListReq extends BasePageReq<SmsLog> {

    @Schema(description = "搜索关键词")
    @Size(max = 64)
    private String keyword;

    @Schema(description = "发送状态")
    private Short status;

    @Schema(description = "短信配置key")
    private String configKey;

}
