package cn.projectan.strix.model.request.system.module.sms;

import cn.projectan.strix.model.db.system.SmsConfig;
import cn.projectan.strix.model.request.base.BasePageReq;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * @author ProjectAn
 * @since 2023/5/20 19:07
 */
@Schema(description = "短信配置列表请求")
@Data
public class SmsConfigListReq extends BasePageReq<SmsConfig> {

    @Schema(description = "搜索关键词")
    @Size(max = 64)
    private String keyword;

}
