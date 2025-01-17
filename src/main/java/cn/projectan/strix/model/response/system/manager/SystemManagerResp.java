package cn.projectan.strix.model.response.system.manager;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * @author ProjectAn
 * @since 2021/7/16 16:15
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SystemManagerResp {

    private String id;

    private String nickname;

    private String loginName;

    private Integer status;

    private Integer type;

    private String regionId;

    private LocalDateTime createdTime;

    private String roleIds;

}
