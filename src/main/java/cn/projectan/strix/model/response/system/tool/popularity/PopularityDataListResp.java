package cn.projectan.strix.model.response.system.tool.popularity;

import cn.projectan.strix.model.db.system.PopularityData;
import cn.projectan.strix.model.response.base.BasePageResp;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.stream.Collectors;

/**
 * @author ProjectAn
 * @since 2023/10/5 22:48
 */
@Schema(description = "热度数据列表响应")
@Getter
public class PopularityDataListResp extends BasePageResp {

    @Schema(description = "数据列表")
    private final List<PopularityDataItem> items;

    public PopularityDataListResp(Page<PopularityData> page) {
        setTotal(page.getTotal());
        items = page.getRecords().stream()
                .map(item -> new PopularityDataItem(item.getId(), item.getConfigKey(), item.getDataId(), item.getOriginalValue()))
                .collect(Collectors.toList());
    }

    @Schema(description = "热度数据列表项")
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PopularityDataItem {

        @Schema(description = "数据ID")
        private String id;

        @Schema(description = "配置标识")
        private String configKey;

        @Schema(description = "数据标识")
        private String dataId;

        @Schema(description = "原始值")
        private Long originalValue;

    }

}
