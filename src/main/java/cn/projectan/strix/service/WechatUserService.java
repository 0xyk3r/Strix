package cn.projectan.strix.service;

import cn.projectan.strix.model.db.WechatUser;
import cn.projectan.strix.model.wechat.WechatConfigBean;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * <p>
 * 服务类
 * </p>
 *
 * @author 安炯奕
 * @since 2021-08-25
 */
public interface WechatUserService extends IService<WechatUser> {

    WechatUser createWechatUser(String openId, WechatConfigBean wechatConfig);

}
