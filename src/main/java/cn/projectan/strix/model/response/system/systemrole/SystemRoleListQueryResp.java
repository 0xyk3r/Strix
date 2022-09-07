package cn.projectan.strix.model.response.system.systemrole;

import cn.projectan.strix.model.db.SystemRole;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * @author 安炯奕
 * @date 2021/7/1 16:37
 */
@Getter
@NoArgsConstructor
public class SystemRoleListQueryResp {

    private List<SystemRoleItem> systemRoleList = new ArrayList<>();

    public SystemRoleListQueryResp(List<SystemRole> roles) {
        for (SystemRole sr : roles) {
            SystemRoleItem item = new SystemRoleItem(sr.getId(), sr.getName());
            systemRoleList.add(item);
        }
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SystemRoleItem {

        private String id;

        private String name;

    }

}
