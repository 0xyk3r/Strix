package cn.projectan.strix.model.response.system.systemregion;

import cn.projectan.strix.model.db.SystemRegion;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author 安炯奕
 * @date 2021/9/29 19:22
 */
@Getter
@NoArgsConstructor
public class SystemRegionChildrenListResp {

    private List<SystemRegionItem> children = new ArrayList<>();

    public SystemRegionChildrenListResp(Collection<SystemRegion> data) {
        children = data.stream().map(r -> new SystemRegionItem(r.getId(), r.getName(), r.getLevel(), r.getParentId(), r.getFullPath(), r.getFullName(), r.getRemarks())).collect(Collectors.toList());
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SystemRegionItem {

        private String id;

        private String name;

        private Integer level;

        private String parentId;

        private String fullPath;

        private String fullName;

        private String remarks;

        private boolean hasChildren = true;

        public SystemRegionItem(String id, String name, Integer level, String parentId, String fullPath, String fullName, String remarks) {
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
