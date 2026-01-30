package cn.projectan.strix.model.response.system.dict;

import cn.projectan.strix.model.db.system.DictData;
import cn.projectan.strix.model.response.base.BasePageResp;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author ProjectAn
 * @since 2023/5/28 23:27
 */
@Getter
@NoArgsConstructor
@Schema(description = "系统 - 字典 - 查询字典数据列表")
public class DictDataListResp extends BasePageResp {

    @Schema(description = "字典数据列表")
    private List<DictDataItem> items = new ArrayList<>();

    public DictDataListResp(List<DictData> data, long total) {
        items = data.stream().map(d ->
                new DictDataItem(d.getId(), d.getKey(), d.getValue(), d.getLabel(), d.getSort(), d.getStyle(), d.getStatus(), d.getRemark())
        ).collect(Collectors.toList());
        this.setTotal(total);
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "字典数据项")
    public static class DictDataItem {

        @Schema(description = "字典数据 ID")
        private String id;

        @Schema(description = "字典数据 Key")
        private String key;

        @Schema(description = "字典数据值")
        private String value;

        @Schema(description = "字典数据标签")
        private String label;

        @Schema(description = "字典数据排序")
        private Short sort;

        @Schema(description = "字典数据样式")
        private String style;

        @Schema(description = "字典数据状态, 使用字典 `CommonSwitch`")
        private Short status;

        @Schema(description = "字典数据备注")
        private String remark;

    }

}
