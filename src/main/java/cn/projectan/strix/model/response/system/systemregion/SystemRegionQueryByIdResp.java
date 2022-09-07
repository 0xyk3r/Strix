package cn.projectan.strix.model.response.system.systemregion;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author 安炯奕
 * @date 2021/9/29 18:06
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SystemRegionQueryByIdResp {

    private String id;

    private String name;

    private Integer level;

    private String parentId;

    private String fullPath;

    private String fullName;

    private String remarks;

}
