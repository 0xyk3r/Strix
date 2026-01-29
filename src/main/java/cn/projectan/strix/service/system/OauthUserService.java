package cn.projectan.strix.service.system;

import cn.projectan.strix.mapper.system.OauthUserMapper;
import cn.projectan.strix.model.db.system.OauthUser;
import cn.projectan.strix.model.db.system.SystemUser;
import cn.projectan.strix.model.other.system.module.oauth.BaseOAuthUserInfo;
import cn.projectan.strix.util.common.SnowflakeUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * <p>
 * Strix OAuth 第三方用户信息 服务类
 * </p>
 *
 * @author ProjectAn
 * @since 2024-04-08
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OauthUserService extends ServiceImpl<OauthUserMapper, OauthUser> {

    private final SystemUserService systemUserService;

    /**
     * 根据第三方用户信息创建系统用户
     *
     * @param oauthUserInfo 第三方用户信息
     * @param platform      平台
     * @return 系统用户
     */
    @Transactional(rollbackFor = Exception.class)
    public SystemUser createSystemUser(BaseOAuthUserInfo oauthUserInfo, Short platform) {
        OauthUser oauthUser = new OauthUser();
        oauthUser.setConfigId(oauthUserInfo.getConfigId());
        oauthUser.setAppId(oauthUserInfo.getAppId());
        oauthUser.setOpenId(oauthUserInfo.getOpenId());
        oauthUser.setUnionId(oauthUserInfo.getUnionId());
        oauthUser.setPlatform(platform);
        save(oauthUser);

        SystemUser systemUser = systemUserService.createSystemUser("u" + SnowflakeUtil.nextSystemUserId(), null);
        systemUserService.bindThirdUser(systemUser.getId(), platform, oauthUser.getId());

        return systemUser;
    }

}
