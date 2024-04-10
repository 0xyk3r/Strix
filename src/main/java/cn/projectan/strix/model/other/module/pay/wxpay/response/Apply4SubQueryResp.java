package cn.projectan.strix.model.other.module.pay.wxpay.response;

import lombok.Data;
import lombok.experimental.Accessors;

import java.util.List;

/**
 * 特约商户进件-查询申请单状态 响应
 *
 * @author ProjectAn
 * @date 2022/7/29 10:47
 */
@Data
@Accessors(chain = true)
public class Apply4SubQueryResp {

    /**
     * 业务申请编号
     */
    private String business_code;

    /**
     * 微信支付申请单号
     */
    private String applyment_id;

    /**
     * 特约商户号
     * 当申请单状态为APPLYMENT_STATE_FINISHED时才返回
     */
    private String sub_mchid;

    /**
     * 超级管理员签约链接
     * 1、超级管理员用微信扫码，关注“微信支付商家助手”公众号后，公众号将自动发送签约消息；超管需点击消息，根据指引完成核对联系信息、账户验证、签约等操作。
     * 2、超管完成核对联系信息，后续申请单进度可通过公众号自动通知超级管理员。
     */
    private String sign_url;

    /**
     * 申请单状态
     * 1、APPLYMENT_STATE_EDITTING（编辑中）：提交申请发生错误导致，请尝试重新提交。
     * 2、APPLYMENT_STATE_AUDITING（审核中）：申请单正在审核中，超级管理员用微信打开“签约链接”，完成绑定微信号后，申请单进度将通过微信公众号通知超级管理员，引导完成后续步骤。
     * 3、APPLYMENT_STATE_REJECTED（已驳回）：请按照驳回原因修改申请资料，超级管理员用微信打开“签约链接”，完成绑定微信号，后续申请单进度将通过微信公众号通知超级管理员。
     * 4、APPLYMENT_STATE_TO_BE_CONFIRMED（待账户验证）：请超级管理员使用微信打开返回的“签约链接”，根据页面指引完成账户验证。
     * 5、APPLYMENT_STATE_TO_BE_SIGNED（待签约）：请超级管理员使用微信打开返回的“签约链接”，根据页面指引完成签约。
     * 6、APPLYMENT_STATE_SIGNING（开通权限中）：系统开通相关权限中，请耐心等待。
     * 7、APPLYMENT_STATE_FINISHED（已完成）：商户入驻申请已完成。
     * 8、APPLYMENT_STATE_CANCELED（已作废）：申请单已被撤销。
     */
    private String applyment_state;

    /**
     * 申请状态描述
     */
    private String applyment_state_msg;

    /**
     * 驳回原因详情
     */
    private List<AuditDetail> audit_detail;

    @Data
    public static class AuditDetail {

        /**
         * 字段名
         */
        private String field;

        /**
         * 字段名称
         */
        private String field_name;

        /**
         * 驳回原因
         */
        private String reject_reason;

    }

}
