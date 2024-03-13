package cn.projectan.strix.model.response.system.role;

import cn.projectan.strix.model.db.SystemRole;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * @author ProjectAn
 * @date 2021/7/1 16:37
 */
@Getter
@NoArgsConstructor
public class SystemRoleListResp {

    private final List<SystemRoleItem> systemRoleList = new ArrayList<>();

    public SystemRoleListResp(List<SystemRole> roles) {
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
