package cn.projectan.strix.model.request.api.wechat;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author ProjectAn
 * @since 2026/1/29 09:33
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class WechatMPAuthReq {

    @NotBlank(message = "授权码不能为空")
    private String code;

}
