package cn.projectan.strix.model.response.system.module.sms;

import cn.projectan.strix.model.db.system.SmsSign;
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
 * @since 2023/5/20 19:27
 */
@Schema(description = "短信签名列表响应")
@Getter
@NoArgsConstructor
public class SmsSignListResp extends BasePageResp {

    @Schema(description = "签名列表")
    private List<SmsSignItem> signs = new ArrayList<>();

    public SmsSignListResp(List<SmsSign> data, Long total) {
        signs = data.stream().map(d ->
                new SmsSignItem(d.getId(), d.getConfigKey(), d.getName(), d.getStatus(), d.getCreatedTime())
        ).collect(Collectors.toList());
        this.setTotal(total);
    }

    @Schema(description = "短信签名列表项")
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SmsSignItem {

        @Schema(description = "签名ID")
        private String id;

        @Schema(description = "配置标识")
        private String configKey;

        @Schema(description = "签名名称")
        private String name;

        @Schema(description = "签名状态")
        private Short status;

        @Schema(description = "创建时间")
        private LocalDateTime createdTime;

    }

}
