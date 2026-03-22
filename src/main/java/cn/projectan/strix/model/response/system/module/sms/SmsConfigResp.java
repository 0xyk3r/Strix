package cn.projectan.strix.model.response.system.module.sms;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * @author ProjectAn
 * @since 2023/5/20 19:19
 */
@Schema(description = "短信配置详情响应")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SmsConfigResp {

    @Schema(description = "配置ID")
    private String id;

    @Schema(description = "配置标识")
    private String key;

    @Schema(description = "配置名称")
    private String name;

    @Schema(description = "平台类型")
    private Short platform;

    @Schema(description = "区域ID")
    private String regionId;

    @Schema(description = "访问密钥")
    private String accessKey;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "创建时间")
    private LocalDateTime createdTime;

    @Schema(description = "签名列表")
    private List<SmsSignListResp.SmsSignItem> signs;

    @Schema(description = "模板列表")
    private List<SmsTemplateListResp.SmsTemplateItem> templates;

}
