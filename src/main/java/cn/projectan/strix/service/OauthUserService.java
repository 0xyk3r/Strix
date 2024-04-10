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

    SystemUser createSystemUser(BaseOAuthUserInfo oauthUserInfo, Integer platform);

}
