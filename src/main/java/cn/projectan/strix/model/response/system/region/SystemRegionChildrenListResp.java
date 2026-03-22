package cn.projectan.strix.model.response.system.region;

import cn.projectan.strix.model.db.system.SystemRegion;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author ProjectAn
 * @since 2021/9/29 19:22
 */
@Schema(description = "地区子级列表响应")
@Getter
@NoArgsConstructor
public class SystemRegionChildrenListResp {

    @Schema(description = "子级区划列表")
    private List<SystemRegionChildItem> children = new ArrayList<>();

    public SystemRegionChildrenListResp(Collection<SystemRegion> data) {
        children = data.stream().map(r ->
                new SystemRegionChildItem(r.getId(), r.getName(), r.getLevel(), r.getParentId(), r.getFullPath(), r.getFullName(), r.getRemarks())
        ).collect(Collectors.toList());
    }

    @Schema(description = "地区子级项")
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SystemRegionChildItem {

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

        public SystemRegionChildItem(String id, String name, Short level, String parentId, String fullPath, String fullName, String remarks) {
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
