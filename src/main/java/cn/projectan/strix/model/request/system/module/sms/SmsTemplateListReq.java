package cn.projectan.strix.model.request.system.module.sms;

import cn.projectan.strix.model.db.system.SmsTemplate;
import cn.projectan.strix.model.request.base.BasePageReq;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * @author ProjectAn
 * @since 2023/5/20 20:59
 */
@Schema(description = "短信模板列表请求")
@Data
public class SmsTemplateListReq extends BasePageReq<SmsTemplate> {

    @Schema(description = "搜索关键词")
    @Size(max = 64)
    private String keyword;

    @Schema(description = "模板类型")
    private Short type;

    @Schema(description = "模板状态")
    private Short status;

    @Schema(description = "短信配置key")
    private String configKey;

}
