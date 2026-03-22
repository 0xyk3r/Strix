package cn.projectan.strix.model.request.api.wechat;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author ProjectAn
 * @since 2026/1/29 09:33
 */
@Schema(description = "微信小程序授权请求")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class WechatMPAuthReq {

    @Schema(description = "微信小程序授权码", example = "0a1B2c3D4e5F6g")
    @NotBlank(message = "授权码不能为空")
    private String code;

}
