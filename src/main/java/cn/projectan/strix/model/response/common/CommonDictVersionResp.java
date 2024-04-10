package cn.projectan.strix.model.response.common;

import cn.projectan.strix.model.db.Dict;
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
 * @date 2023/5/29 11:26
 */
@Getter
@NoArgsConstructor
public class CommonDictVersionResp implements java.io.Serializable {

    @Serial
    private static final long serialVersionUID = 2L;

    private List<DictVersionItem> items = new ArrayList<>();

    public CommonDictVersionResp(List<Dict> data) {
        items = data.stream().map(d -> new DictVersionItem(d.getKey(), d.getVersion())).collect(Collectors.toList());
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DictVersionItem {

        private String key;

        private Integer version;

    }

}
