package cn.projectan.strix.model.response.system.systemmanager;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * @author 安炯奕
 * @date 2021/7/16 16:15
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SystemManagerQueryByIdResp {

    private String id;

    private String nickname;

    private String loginName;

    private Integer managerStatus;

    private Integer managerType;

    private String regionId;

    private LocalDateTime createTime;

    private String roleIds;

}
