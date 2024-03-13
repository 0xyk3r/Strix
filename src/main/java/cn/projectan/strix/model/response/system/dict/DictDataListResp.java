package cn.projectan.strix.model.response.system.dict;

import cn.projectan.strix.model.db.DictData;
import cn.projectan.strix.model.response.base.BasePageResp;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * @author ProjectAn
 * @date 2023/5/28 23:27
 */
@Getter
@NoArgsConstructor
public class DictDataListResp extends BasePageResp {

    private List<DictDataItem> items = new ArrayList<>();

    public DictDataListResp(List<DictData> data, long total) {
        items = data.stream().map(d -> new DictDataItem(d.getId(), d.getKey(), d.getValue(), d.getLabel(), d.getSort(), d.getStyle(), d.getStatus(), d.getRemark())).toList();
        this.setTotal(total);
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DictDataItem {

        private String id;

        private String key;

        private String value;

        private String label;

        private Integer sort;

        private String style;

        private Integer status;

        private String remark;

    }

}
