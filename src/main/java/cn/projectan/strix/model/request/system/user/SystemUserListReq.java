package cn.projectan.strix.model.request.system.user;

import cn.projectan.strix.model.db.system.SystemUser;
import cn.projectan.strix.model.request.base.BasePageReq;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * @author ProjectAn
 * @since 2021/8/27 15:44
 */
@Schema(description = "用户列表请求")
@Data
public class SystemUserListReq extends BasePageReq<SystemUser> {

    @Schema(description = "搜索关键词")
    @Size(max = 64)
    private String keyword;

    @Schema(description = "用户状态")
    private Short status;

}
