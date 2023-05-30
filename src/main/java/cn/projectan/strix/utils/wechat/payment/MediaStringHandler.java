package cn.projectan.strix.utils.wechat.payment;

import cn.projectan.strix.core.module.payment.wxpay.WxPayTools;
import cn.projectan.strix.model.wechat.payment.WxPayConfig;

import java.io.File;

/**
 * 微信支付v3接口 图片MediaID
 *
 * @author 安炯奕
 * @date 2022/7/22 17:59
 */
public class MediaStringHandler {

    private final WxPayConfig wxPayConfig;

    public MediaStringHandler(WxPayConfig wxPayConfig) {
        this.wxPayConfig = wxPayConfig;
    }

    public String handle(File file, boolean deleteAfterHandle) {
        return WxPayTools.merchantUploadMedia(file, deleteAfterHandle, wxPayConfig);
    }

}
