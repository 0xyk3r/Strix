package cn.projectan.strix.model.response.system.permission;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author ProjectAn
 * @date 2021/7/6 14:29
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SystemPermissionResp {

    private String id;

    private String name;

    private String key;

    private String menuId;

    private String description;

}
