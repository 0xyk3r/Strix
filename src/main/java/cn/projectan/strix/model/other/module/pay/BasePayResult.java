package cn.projectan.strix.model.other.module.pay;

import lombok.Data;

/**
 * 支付结果
 *
 * @author ProjectAn
 * @date 2024/4/13 下午5:26
 */
@Data
public class BasePayResult {

    private Boolean success;

    private String orderId;

    private String platformOrderNo;

    private Integer totalAmount;

    private String platformUserId;

    private String attach;

    private String originalResult;

}
