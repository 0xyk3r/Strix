package cn.projectan.strix.service;

import cn.projectan.strix.model.db.OauthUser;
import cn.projectan.strix.model.db.SystemUser;
import cn.projectan.strix.model.other.module.oauth.BaseOAuthUserInfo;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * <p>
 * Strix OAuth 第三方用户信息 服务类
 * </p>
 *
 * @author ProjectAn
 * @since 2024-04-08
 */
public interface OauthUserService extends IService<OauthUser> {

    /**
     * 根据第三方用户信息创建系统用户
     *
     * @param oauthUserInfo 第三方用户信息
     * @param platform      平台
     * @return 系统用户
     */
    SystemUser createSystemUser(BaseOAuthUserInfo oauthUserInfo, Integer platform);

}
