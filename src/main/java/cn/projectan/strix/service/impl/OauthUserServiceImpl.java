package cn.projectan.strix.service.impl;

import cn.projectan.strix.mapper.OauthUserMapper;
import cn.projectan.strix.model.db.OauthUser;
import cn.projectan.strix.model.db.SystemUser;
import cn.projectan.strix.model.other.module.oauth.BaseOAuthUserInfo;
import cn.projectan.strix.service.OauthUserService;
import cn.projectan.strix.service.SystemUserService;
import cn.projectan.strix.utils.SnowflakeUtil;
import cn.projectan.strix.utils.SpringUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * <p>
 * Strix OAuth 第三方用户信息 服务实现类
 * </p>
 *
 * @author ProjectAn
 * @since 2024-04-08
 */
@Service
@RequiredArgsConstructor
public class OauthUserServiceImpl extends ServiceImpl<OauthUserMapper, OauthUser> implements OauthUserService {

    private final SystemUserService systemUserService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SystemUser createSystemUser(BaseOAuthUserInfo oauthUserInfo, Integer platform) {
        OauthUserServiceImpl proxy = SpringUtil.getAopProxy(this);

        OauthUser oauthUser = new OauthUser();
        oauthUser.setConfigId(oauthUserInfo.getConfigId());
        oauthUser.setAppId(oauthUserInfo.getAppId());
        oauthUser.setOpenId(oauthUserInfo.getOpenId());
        oauthUser.setUnionId(oauthUserInfo.getUnionId());
        oauthUser.setPlatform(platform);
        proxy.save(oauthUser);

        SystemUser systemUser = systemUserService.createSystemUser("u" + SnowflakeUtil.nextSystemUserId(), null);
        systemUserService.bindThirdUser(systemUser.getId(), platform, oauthUser.getId());

        return systemUser;
    }

}
