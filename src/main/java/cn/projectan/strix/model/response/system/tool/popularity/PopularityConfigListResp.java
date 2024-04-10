package cn.projectan.strix.model.response.system.tool.popularity;

import cn.projectan.strix.model.db.PopularityConfig;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.stream.Collectors;

/**
 * @author ProjectAn
 * @date 2023/10/5 21:41
 */
@Getter
public class PopularityConfigListResp {

    private final List<PopularityConfigItem> items;

    public PopularityConfigListResp(List<PopularityConfig> data) {
        items = data.stream()
                .map(item -> new PopularityConfigItem(item.getId(), item.getName()))
                .collect(Collectors.toList());
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PopularityConfigItem {

        private String id;

        private String name;

    }

}
