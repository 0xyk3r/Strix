package cn.projectan.strix.model.db;

import cn.projectan.strix.model.db.base.BaseModel;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;
import java.time.LocalDateTime;

/**
 * <p>
 *
 * </p>
 *
 * @author ProjectAn
 * @since 2021-08-24
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_pay_order")
public class PayOrder extends BaseModel {

    @Serial
    private static final long serialVersionUID = 2L;

    /**
     * 支付配置ID
     */
    private String configId;

    /**
     * 支付平台
     */
    private Integer platform;

    /**
     * 业务处理器ID
     */
    private String handlerId;

    /**
     * 支付参数
     */
    private String params;

    /**
     * 支付状态
     *
     * @see cn.projectan.strix.model.dict.PayOrderStatus
     */
    @TableField("`status`")
    private Integer status;

    /**
     * 支付内容标题
     */
    private String title;

    /**
     * 支付成功后回调数据
     */
    private String attach;

    /**
     * 订单过期时间
     */
    private LocalDateTime expireTime;

    /**
     * 支付成功时间
     */
    private LocalDateTime payTime;

    /**
     * 支付回调内容
     */
    private String notifyContent;

    /**
     * 支付订单总金额
     */
    private Integer totalAmount;

    /**
     * 已经支付的金额
     */
    private Integer totalPayAmount;

    /**
     * 已经退款的总金额
     */
    private Integer totalRefundAmount;

}
