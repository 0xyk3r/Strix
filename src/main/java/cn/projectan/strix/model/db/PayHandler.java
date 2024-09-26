package cn.projectan.strix.model.db;

import cn.projectan.strix.model.db.base.BaseModel;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.io.Serial;

/**
 * <p>
 * Strix Pay 订单处理器
 * </p>
 *
 * @author ProjectAn
 * @since 2024-04-13
 */
@Getter
@Setter
@Accessors(chain = true)
@NoArgsConstructor
@AllArgsConstructor
@TableName("sys_pay_handler")
public class PayHandler extends BaseModel {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 支付处理器名称
     */
    @TableField("`name`")
    private String name;

    /**
     * 支付成功处理器
     */
    private String successHandler;

    /**
     * 支付退款处理器
     */
    private String refundHandler;

    /**
     * 支付超时处理器
     */
    private String timeoutHandler;
}
