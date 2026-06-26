package cn.projectan.strix.model.response.system.manager;

import cn.projectan.strix.model.db.system.SystemManager;
import cn.projectan.strix.model.response.base.BasePageResp;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author ProjectAn
 * @since 2021/6/11 18:22
 */
@Schema(description = "管理员列表响应")
@Getter
@NoArgsConstructor
public class SystemManagerListResp extends BasePageResp {

    @Schema(description = "管理员列表")
    private List<SystemManagerItem> systemManagerList = new ArrayList<>();

    public SystemManagerListResp(List<SystemManager> data, Long total) {
        systemManagerList = data.stream().map(d ->
                new SystemManagerItem(d.getId(), d.getNickname(), d.getLoginName(), d.getStatus(), d.getType(), d.getRegionId(), d.getBuiltin(), d.getCreatedTime(), d.getAvatarConfig())
        ).collect(Collectors.toList());
        this.setTotal(total);
    }

    @Schema(description = "管理员列表项")
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SystemManagerItem {

        @Schema(description = "管理员ID")
        private String id;

        @Schema(description = "管理员昵称")
        private String nickname;

        @Schema(description = "登录名")
        private String loginName;

        @Schema(description = "状态")
        private Short status;

        @Schema(description = "管理员类型")
        private Short type;

        @Schema(description = "所属区域ID")
        private String regionId;

        @Schema(description = "是否系统内置")
        private Short builtin;

        @Schema(description = "创建时间")
        private LocalDateTime createdTime;

        @Schema(description = "DiceBear 头像配置 JSON")
        private String avatarConfig;

    }

}
