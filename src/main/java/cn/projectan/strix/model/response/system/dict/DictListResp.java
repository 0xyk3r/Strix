package cn.projectan.strix.model.response.system.dict;

import cn.projectan.strix.model.db.Dict;
import cn.projectan.strix.model.response.base.BasePageResp;
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
 * @since 2023/5/30 11:01
 */
@Getter
@NoArgsConstructor
public class DictListResp extends BasePageResp {

    private List<DictItem> items = new ArrayList<>();

    public DictListResp(List<Dict> data, long total) {
        items = data.stream().map(d ->
                new DictItem(d.getId(), d.getKey(), d.getName(), d.getDataType(), d.getStatus(), d.getRemark(), d.getVersion(), d.getProvided(), d.getCreatedTime())
        ).collect(Collectors.toList());
        this.setTotal(total);
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DictItem {

        private String id;

        private String key;

        private String name;

        private Integer dataType;

        private Integer status;

        private String remark;

        private Integer version;

        private Integer provided;

        private LocalDateTime createdTime;

    }

}
