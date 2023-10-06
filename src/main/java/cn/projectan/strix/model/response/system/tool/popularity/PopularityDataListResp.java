package cn.projectan.strix.model.response.system.tool.popularity;

import cn.projectan.strix.model.db.PopularityData;
import cn.projectan.strix.model.response.base.BasePageResp;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * @author 安炯奕
 * @date 2023/10/5 22:48
 */
@Getter
public class PopularityDataListResp extends BasePageResp {

    private final List<PopularityDataItem> items;

    public PopularityDataListResp(Page<PopularityData> page) {
        setTotal(page.getTotal());
        items = page.getRecords().stream()
                .map(item -> new PopularityDataItem(item.getId(), item.getConfigKey(), item.getDataId(), item.getOriginalValue()))
                .toList();
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PopularityDataItem {

        private String id;

        private String configKey;

        private String dataId;

        private Integer originalValue;

    }

}
