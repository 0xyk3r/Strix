package cn.projectan.strix.model.response.system.tool.popularity;

import cn.projectan.strix.model.db.system.PopularityConfig;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * @author ProjectAn
 * @since 2023/10/5 21:45
 */
@Schema(description = "热度配置详情响应")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PopularityConfigResp {

    @Schema(description = "配置ID")
    private String id;

    @Schema(description = "配置名称")
    private String name;

    @Schema(description = "配置标识")
    private String configKey;

    @Schema(description = "初始值")
    private Long initialValue;

    @Schema(description = "额外值")
    private Long extraValue;

    @Schema(description = "放大倍率")
    private BigDecimal magValue;

    public PopularityConfigResp(PopularityConfig data) {
        this.id = data.getId();
        this.name = data.getName();
        this.configKey = data.getConfigKey();
        this.initialValue = data.getInitialValue();
        this.extraValue = data.getExtraValue();
        this.magValue = data.getMagValue();
    }

}
