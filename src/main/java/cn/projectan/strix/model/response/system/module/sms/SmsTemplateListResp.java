package cn.projectan.strix.model.response.system.module.sms;

import cn.projectan.strix.model.db.system.SmsTemplate;
import cn.projectan.strix.model.response.base.BasePageResp;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author ProjectAn
 * @since 2023/5/20 19:31
 */
@Schema(description = "短信模板列表响应")
@Getter
@NoArgsConstructor
public class SmsTemplateListResp extends BasePageResp {

    @Schema(description = "模板列表")
    private List<SmsTemplateItem> templates = new ArrayList<>();

    public SmsTemplateListResp(List<SmsTemplate> data, Long total) {
        templates = data.stream().map(d ->
                new SmsTemplateItem(d.getId(), d.getConfigKey(), d.getCode(), d.getName(), d.getType(), d.getStatus(), d.getContent(), d.getCreatedTime())
        ).collect(Collectors.toList());
        this.setTotal(total);
    }

    @Schema(description = "短信模板列表项")
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SmsTemplateItem {

        @Schema(description = "模板ID")
        private String id;

        @Schema(description = "配置标识")
        private String configKey;

        @Schema(description = "模板编码")
        private String code;

        @Schema(description = "模板名称")
        private String name;

        @Schema(description = "模板类型")
        private Short type;

        @Schema(description = "模板状态")
        private Short status;

        @Schema(description = "模板内容")
        private String content;

        @Schema(description = "创建时间")
        private LocalDateTime createdTime;

    }
}
