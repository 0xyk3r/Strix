package cn.projectan.strix.model.request.system.profile;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 更新头像配置请求
 *
 * @author ProjectAn
 */
@Schema(description = "更新头像配置请求")
@Data
public class ProfileAvatarUpdateReq {

    @Schema(description = "DiceBear 头像配置 JSON，传 null 或空字符串则重置为默认")
    @Size(max = 4096, message = "头像配置数据过长")
    private String avatarConfig;

}
