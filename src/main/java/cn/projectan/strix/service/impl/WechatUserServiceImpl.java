package cn.projectan.strix.service.impl;

import cn.projectan.strix.mapper.WechatUserMapper;
import cn.projectan.strix.model.db.SystemUser;
import cn.projectan.strix.model.db.WechatUser;
import cn.projectan.strix.model.wechat.WechatConfigBean;
import cn.projectan.strix.service.SystemUserService;
import cn.projectan.strix.service.WechatUserService;
import cn.projectan.strix.utils.SpringUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * <p>
 * 服务实现类
 * </p>
 *
 * @author ProjectAn
 * @since 2021-08-25
 */
@Service
@RequiredArgsConstructor
public class WechatUserServiceImpl extends ServiceImpl<WechatUserMapper, WechatUser> implements WechatUserService {

    private final SystemUserService systemUserService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public WechatUser createWechatUser(String openId, WechatConfigBean wechatConfig) {
        WechatUser wechatUser = new WechatUser();
        wechatUser.setConfigId(wechatConfig.getId());
        wechatUser.setAppId(wechatConfig.getAppId());
        wechatUser.setOpenId(openId);
        wechatUser.setCreateBy("WechatAuth");
        wechatUser.setUpdateBy("WechatAuth");
        SpringUtil.getAopProxy(this).save(wechatUser);

        SystemUser systemUser = systemUserService.createSystemUser("wx" + openId, null);
        systemUserService.bindThirdUser(systemUser.getId(), 1, wechatUser.getId());

        return wechatUser;
    }

}
