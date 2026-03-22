package cn.projectan.strix.model.response.system.dict;

import cn.projectan.strix.model.db.system.Dict;
import cn.projectan.strix.model.response.base.BasePageResp;
import io.swagger.v3.oas.annotations.media.Schema;
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
@Schema(description = "字典列表响应")
@Getter
@NoArgsConstructor
public class DictListResp extends BasePageResp {

    @Schema(description = "字典项列表")
    private List<DictItem> items = new ArrayList<>();

    public DictListResp(List<Dict> data, long total) {
        items = data.stream().map(d ->
                new DictItem(d.getId(), d.getKey(), d.getName(), d.getDataType(), d.getStatus(), d.getRemark(), d.getVersion(), d.getProvided(), d.getCreatedTime())
        ).collect(Collectors.toList());
        this.setTotal(total);
    }

    @Schema(description = "字典列表项")
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DictItem {

        @Schema(description = "字典ID")
        private String id;

        @Schema(description = "字典键")
        private String key;

        @Schema(description = "字典名称")
        private String name;

        @Schema(description = "数据类型")
        private Short dataType;

        @Schema(description = "状态")
        private Short status;

        @Schema(description = "备注")
        private String remark;

        @Schema(description = "版本号")
        private Integer version;

        @Schema(description = "是否系统内置")
        private Short provided;

        @Schema(description = "创建时间")
        private LocalDateTime createdTime;

    }

}
