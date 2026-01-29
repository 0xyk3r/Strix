package cn.projectan.strix.model.other.system.module.oauth.wechat.mp.resp;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * @author ProjectAn
 * @since 2026/1/29 09:54
 */
@Data
public class WechatMPAuthResp {

    @JsonProperty("session_key")
    private String sessionKey;

    @JsonProperty("unionid")
    private String unionId;

    @JsonProperty("openid")
    private String openId;

    @JsonProperty("errcode")
    private String errCode;

    @JsonProperty("errmsg")
    private String errMsg;

}
