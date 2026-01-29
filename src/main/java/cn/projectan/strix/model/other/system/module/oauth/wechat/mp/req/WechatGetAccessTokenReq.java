package cn.projectan.strix.model.other.system.module.oauth.wechat.mp.req;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author ProjectAn
 * @since 2026/1/29 10:49
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class WechatGetAccessTokenReq {

    @JsonProperty("grant_type")
    private String grantType;

    @JsonProperty("appid")
    private String appId;

    @JsonProperty("secret")
    private String secret;

    @JsonProperty("force_refresh")
    private Boolean forceRefresh;

}
