package cn.projectan.strix.service.system;

import cn.projectan.strix.mapper.system.OauthUserMapper;
import cn.projectan.strix.model.db.system.OauthUser;
import cn.projectan.strix.model.db.system.SystemUser;
import cn.projectan.strix.model.db.system.SystemUserRelation;
import cn.projectan.strix.model.other.system.module.oauth.BaseOAuthUserInfo;
import cn.projectan.strix.util.common.NicknameGenerator;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

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
    private final SystemUserRelationService systemUserRelationService;

    /**
     * 根据appId和openId查询OAuth用户
     *
     * @param appId  应用ID
     * @param openId 开放ID
     * @return OAuth用户
     */
    public OauthUser getByAppIdAndOpenId(String appId, String openId) {
        return lambdaQuery()
                .eq(OauthUser::getAppId, appId)
                .eq(OauthUser::getOpenId, openId)
                .one();
    }

    public SystemUser loginThirdUser(BaseOAuthUserInfo info, Short platform) {
        OauthUser oauthUser = lambdaQuery()
                .eq(OauthUser::getPlatform, platform)
                .and(wrapper -> wrapper.eq(OauthUser::getOpenId, info.getOpenId())
                        .or().eq(StringUtils.hasText(info.getUnionId()), OauthUser::getUnionId, info.getUnionId()))
                .one();
        if (oauthUser == null) {
            return null;
        }

        String systemUserId = systemUserRelationService.lambdaQuery()
                .select(SystemUserRelation::getSystemUserId)
                .eq(SystemUserRelation::getRelationType, platform)
                .eq(SystemUserRelation::getRelationId, oauthUser.getId())
                .oneOpt()
                .map(SystemUserRelation::getSystemUserId)
                .orElse(null);
        if (systemUserId == null) {
            return null;
        }

        return systemUserService.getById(systemUserId);
    }

    /**
     * 根据第三方用户信息登录或创建系统用户
     *
     * @param info     第三方用户信息
     * @param platform 平台
     * @return 系统用户
     */
    @Transactional(rollbackFor = Exception.class)
    public SystemUser loginOrCreateSystemUser(BaseOAuthUserInfo info, Short platform) {
        // 查找已有的 OAuth 用户
        OauthUser oauthUser = lambdaQuery()
                .eq(OauthUser::getPlatform, platform)
                .and(wrapper -> wrapper.eq(OauthUser::getOpenId, info.getOpenId())
                        .or().eq(StringUtils.hasText(info.getUnionId()), OauthUser::getUnionId, info.getUnionId()))
                .one();

        if (oauthUser != null) {
            // OAuth 用户已存在，查找关联的系统用户
            String systemUserId = systemUserRelationService.lambdaQuery()
                    .select(SystemUserRelation::getSystemUserId)
                    .eq(SystemUserRelation::getRelationType, platform)
                    .eq(SystemUserRelation::getRelationId, oauthUser.getId())
                    .oneOpt()
                    .map(SystemUserRelation::getSystemUserId)
                    .orElse(null);

            if (systemUserId != null) {
                SystemUser systemUser = systemUserService.getById(systemUserId);
                if (systemUser != null) {
                    return systemUser;
                }
                // 系统用户已被删除，清理失效关联
                systemUserRelationService.lambdaUpdate()
                        .eq(SystemUserRelation::getSystemUserId, systemUserId)
                        .eq(SystemUserRelation::getRelationId, oauthUser.getId())
                        .remove();
            }

            // 关联不存在或系统用户已被删除，创建新系统用户并绑定到已有 OAuth 用户
            SystemUser systemUser = systemUserService.createSystemUser(NicknameGenerator.generateWithPaddedSuffix(), null);
            systemUserService.bindThirdUser(systemUser.getId(), platform, oauthUser.getId());
            return systemUser;
        }

        // OAuth 用户不存在，创建完整的新用户
        oauthUser = new OauthUser();
        oauthUser.setConfigId(info.getConfigId());
        oauthUser.setAppId(info.getAppId());
        oauthUser.setOpenId(info.getOpenId());
        oauthUser.setUnionId(info.getUnionId());
        oauthUser.setPlatform(platform);
        save(oauthUser);

        SystemUser systemUser = systemUserService.createSystemUser(NicknameGenerator.generateWithPaddedSuffix(), null);
        systemUserService.bindThirdUser(systemUser.getId(), platform, oauthUser.getId());
        return systemUser;
    }

}
