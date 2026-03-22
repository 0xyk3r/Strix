package cn.projectan.strix.model.response.system.region;

import cn.projectan.strix.model.db.system.SystemRegion;
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
 * @since 2021/9/29 17:58
 */
@Schema(description = "地区列表响应")
@Getter
@NoArgsConstructor
public class SystemRegionListResp extends BasePageResp {

    @Schema(description = "地区列表")
    private List<SystemRegionListItem> systemRegionList = new ArrayList<>();

    public SystemRegionListResp(List<SystemRegion> data, Long total) {
        systemRegionList = data.stream().map(d ->
                new SystemRegionListItem(d.getId(), d.getName(), d.getLevel(), d.getParentId(), d.getFullPath(), d.getFullName(), d.getRemarks())
        ).collect(Collectors.toList());
        this.setTotal(total);
    }

    @Schema(description = "地区列表项")
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SystemRegionListItem {

        @Schema(description = "区划ID")
        private String id;

        @Schema(description = "区划名称")
        private String name;

        @Schema(description = "区划层级")
        private Short level;

        @Schema(description = "父级区划ID")
        private String parentId;

        @Schema(description = "完整路径")
        private String fullPath;

        @Schema(description = "完整名称")
        private String fullName;

        @Schema(description = "备注")
        private String remarks;

        @Schema(description = "是否有子级")
        private boolean hasChildren = true;

        public SystemRegionListItem(String id, String name, Short level, String parentId, String fullPath, String fullName, String remarks) {
            this.id = id;
            this.name = name;
            this.level = level;
            this.parentId = parentId;
            this.fullPath = fullPath;
            this.fullName = fullName;
            this.remarks = remarks;
        }

    }

}
