package cn.projectan.strix.model.response.common;

import cn.projectan.strix.model.db.system.Dict;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author ProjectAn
 * @since 2023/5/29 11:26
 */
@Getter
@NoArgsConstructor
@Schema(description = "通用 - 字典 - 获取字典版本列表响应")
public class CommonDictVersionResp implements java.io.Serializable {

    @Serial
    private static final long serialVersionUID = 2L;

    @Schema(description = "字典版本列表")
    private List<DictVersionItem> items = new ArrayList<>();

    public CommonDictVersionResp(List<Dict> data) {
        items = data.stream()
                .map(d -> new DictVersionItem(d.getKey(), d.getVersion()))
                .collect(Collectors.toList());
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "字典版本项")
    public static class DictVersionItem {

        @Schema(description = "字典 Key")
        private String key;

        @Schema(description = "字典版本")
        private Integer version;

    }

}
