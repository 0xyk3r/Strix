package cn.projectan.strix.model.other.module.pay.wxpay;

import cn.projectan.strix.model.other.module.pay.PaymentData;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author ProjectAn
 * @date 2024/4/2 0:11
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class WechatPayPaymentData extends PaymentData {

    private String openId;

}
