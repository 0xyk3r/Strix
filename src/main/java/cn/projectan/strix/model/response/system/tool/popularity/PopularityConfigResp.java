package cn.projectan.strix.model.response.system.tool.popularity;

import cn.projectan.strix.model.db.PopularityConfig;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * @author ProjectAn
 * @since 2023/10/5 21:45
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PopularityConfigResp {

    private String id;

    private String name;

    private String configKey;

    private Integer initialValue;

    private Integer extraValue;

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
