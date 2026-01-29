package cn.projectan.strix.model.other.system.module.oauth.wechat.mp.resp;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * @author ProjectAn
 * @since 2026/1/29 11:02
 */
@Data
public class WechatMPGetPhoneNumberResp {

    @JsonProperty("errcode")
    private Integer errCode;

    @JsonProperty("errmsg")
    private String errMsg;

    @JsonProperty("phone_info")
    private PhoneInfo phoneInfo;


    @Data
    public static class PhoneInfo {

        private String phoneNumber;

        private String purePhoneNumber;

        private String countryCode;

        private Watermark watermark;


        @Data
        public static class Watermark {

            private Long timestamp;

            @JsonProperty("appid")
            private String appId;

        }

    }

}
