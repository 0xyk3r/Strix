package cn.projectan.strix.model.response.system.manager;

import cn.projectan.strix.model.db.SystemManager;
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
 * @author 安炯奕
 * @date 2021/6/11 18:22
 */
@Getter
@NoArgsConstructor
public class SystemManagerListResp extends BasePageResp {

    private List<SystemManagerItem> systemManagerList = new ArrayList<>();

    public SystemManagerListResp(List<SystemManager> data, Long total) {
        systemManagerList = data.stream().map(d -> new SystemManagerItem(d.getId(), d.getNickname(), d.getLoginName(), d.getStatus(), d.getType(), d.getRegionId(), d.getCreateTime())).collect(Collectors.toList());
        this.setTotal(total);
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SystemManagerItem {

        private String id;

        private String nickname;

        private String loginName;

        private Integer status;

        private Integer type;

        private String regionId;

        private LocalDateTime createTime;

    }

}
