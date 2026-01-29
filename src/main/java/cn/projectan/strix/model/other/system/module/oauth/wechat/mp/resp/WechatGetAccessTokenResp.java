package cn.projectan.strix.model.other.system.module.oauth.wechat.mp.resp;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * @author ProjectAn
 * @since 2026/1/29 10:53
 */
@Data
public class WechatGetAccessTokenResp {

    @JsonProperty("access_token")
    private String accessToken;

    @JsonProperty("expires_in")
    private Long expiresIn;

}
