package cn.projectan.strix.model.response.common;

import cn.projectan.strix.model.db.Dict;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * @author 安炯奕
 * @date 2023/5/29 11:26
 */
@Getter
@NoArgsConstructor
public class CommonDictVersionResp {

    private List<DictVersionItem> items = new ArrayList<>();

    public CommonDictVersionResp(List<Dict> data) {
        items = data.stream().map(d -> new DictVersionItem(d.getKey(), d.getVersion())).toList();
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DictVersionItem {

        private String key;

        private Integer version;

    }

}
