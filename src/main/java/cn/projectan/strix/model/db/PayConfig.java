package cn.projectan.strix.model.db;

import cn.projectan.strix.model.db.base.BaseModel;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.io.Serial;

/**
 * <p>
 * Strix Pay 配置
 * </p>
 *
 * @author ProjectAn
 * @since 2021-08-24
 */
@Getter
@Setter
@Accessors(chain = true)
@NoArgsConstructor
@AllArgsConstructor
@TableName("sys_pay_config")
public class PayConfig extends BaseModel<PayConfig> {

    @Serial
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
