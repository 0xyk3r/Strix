package cn.projectan.strix.model.response.system.region;

import cn.projectan.strix.model.db.SystemRegion;
import cn.projectan.strix.model.response.base.BasePageResp;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author 安炯奕
 * @date 2021/9/29 17:58
 */
@Getter
@NoArgsConstructor
public class SystemRegionListQueryResp extends BasePageResp {

    private List<SystemRegionItem> systemRegionList = new ArrayList<>();

    public SystemRegionListQueryResp(List<SystemRegion> data, Long total) {
        systemRegionList = data.stream().map(d -> new SystemRegionItem(d.getId(), d.getName(), d.getLevel(), d.getParentId(), d.getFullPath(), d.getFullName(), d.getRemarks())).collect(Collectors.toList());
        this.setTotal(total);
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
