package cn.projectan.strix.model.other.module.pay.wxpay;

import cn.projectan.strix.model.other.module.pay.BasePayParam;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author ProjectAn
 * @since 2024/4/2 0:11
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class WechatPayPayParam extends BasePayParam {

    private String openId;

}
