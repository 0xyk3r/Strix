package cn.projectan.strix.model.response.system.dict;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * @author ProjectAn
 * @since 2026-04-19
 */
@Schema(description = "字典全局搜索结果响应")
@Data
@NoArgsConstructor
public class DictSearchResultResp {

    @Schema(description = "搜索结果列表")
    private List<SearchResultItem> items;

    @Schema(description = "搜索结果项")
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SearchResultItem {
        @Schema(description = "字典 Key")
        private String dictKey;
        @Schema(description = "字典名称")
        private String dictName;
        @Schema(description = "匹配类型: DICT_KEY / DICT_NAME / DATA_LABEL / DATA_VALUE")
        private String matchType;
        @Schema(description = "匹配字段")
        private String matchField;
        @Schema(description = "匹配值")
        private String matchValue;
    }

}
