package cn.projectan.strix.model.other.system.module.oauth.wechat.mp.req;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author ProjectAn
 * @since 2026/1/29 11:02
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class WechatMPGetPhoneNumberReq {

    private String code;

}
