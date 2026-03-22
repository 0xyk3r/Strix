package cn.projectan.strix.model.response.system.tool.popularity;

import cn.projectan.strix.model.db.system.PopularityConfig;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.stream.Collectors;

/**
 * @author ProjectAn
 * @since 2023/10/5 21:41
 */
@Schema(description = "热度配置列表响应")
@Getter
public class PopularityConfigListResp {

    @Schema(description = "配置列表")
    private final List<PopularityConfigItem> items;

    public PopularityConfigListResp(List<PopularityConfig> data) {
        items = data.stream()
                .map(item -> new PopularityConfigItem(item.getId(), item.getName()))
                .collect(Collectors.toList());
    }

    @Schema(description = "热度配置列表项")
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PopularityConfigItem {

        @Schema(description = "配置ID")
        private String id;

        @Schema(description = "配置名称")
        private String name;

    }

}
