package cn.projectan.strix.model.db;

import cn.projectan.strix.model.db.base.BaseModel;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * <p>
 *
 * </p>
 *
 * @author 安炯奕
 * @since 2021-08-24
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_payment_config")
public class PaymentConfig extends BaseModel {

    private static final long serialVersionUID = 1L;

    /**
     * 支付账户名称
     */
    private String name;

    /**
     * 1微信支付 2支付宝 3QQ钱包 4银联 5京东 6PayPal
     */
    private Integer platform;

    /**
     * 序列化后的配置信息
     */
    private String configData;


}
