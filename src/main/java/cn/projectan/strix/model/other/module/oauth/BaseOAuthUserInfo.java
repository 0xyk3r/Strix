package cn.projectan.strix.model.other.module.oauth;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author ProjectAn
 * @date 2024/4/4 1:59
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BaseOAuthUserInfo {

    private String configId;

    private String appId;

    private String accessToken;

    private String refreshToken;

    private String openId;

    private String unionId;

    private Integer expiresIn;

}
