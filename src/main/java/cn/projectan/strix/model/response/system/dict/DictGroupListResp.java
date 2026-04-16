package cn.projectan.strix.model.response.system.dict;

import cn.projectan.strix.model.db.system.DictGroup;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * @author ProjectAn
 * @since 2026-04-19
 */
@Schema(description = "字典分组列表响应")
@Data
@NoArgsConstructor
public class DictGroupListResp {

    @Schema(description = "分组列表")
    private List<DictGroupItem> items;

    public DictGroupListResp(List<DictGroup> groups, List<Long> dictCounts) {
        this.items = new ArrayList<>();
        for (int i = 0; i < groups.size(); i++) {
            DictGroup g = groups.get(i);
            long count = i < dictCounts.size() ? dictCounts.get(i) : 0;
            this.items.add(new DictGroupItem(g.getId(), g.getName(), g.getIcon(), g.getSortValue(), count));
        }
    }

    @Schema(description = "字典分组列表项")
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DictGroupItem {
        @Schema(description = "分组 ID")
        private String id;
        @Schema(description = "分组名称")
        private String name;
        @Schema(description = "分组图标")
        private String icon;
        @Schema(description = "排序值")
        private Short sortValue;
        @Schema(description = "该分组下的字典数量")
        private long dictCount;
    }

}
