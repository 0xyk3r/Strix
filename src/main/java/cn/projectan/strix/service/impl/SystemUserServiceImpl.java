package cn.projectan.strix.service.impl;

import cn.projectan.strix.mapper.SystemUserMapper;
import cn.projectan.strix.model.db.SystemUser;
import cn.projectan.strix.model.db.SystemUserRelation;
import cn.projectan.strix.model.dict.SystemUserStatus;
import cn.projectan.strix.service.SystemUserRelationService;
import cn.projectan.strix.service.SystemUserService;
import cn.projectan.strix.utils.RedisUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

/**
 * <p>
 * 服务实现类
 * </p>
 *
 * @author ProjectAn
 * @since 2021-08-26
 */
@Service
@RequiredArgsConstructor
public class SystemUserServiceImpl extends ServiceImpl<SystemUserMapper, SystemUser> implements SystemUserService {

    private final SystemUserRelationService systemUserRelationService;
    private final RedisUtil redisUtil;

    @Override
    public SystemUser createSystemUser(String nickname, String phoneNumber) {
        Assert.isTrue(
                !lambdaQuery()
                        .eq(SystemUser::getNickname, nickname)
                        .or(q -> q.eq(SystemUser::getPhoneNumber, phoneNumber))
                        .exists(),
                "昵称或手机号码已被使用，请更换后重试");

        SystemUser systemUser = new SystemUser();
        systemUser.setNickname(nickname);
        systemUser.setStatus(SystemUserStatus.NORMAL);
        systemUser.setPhoneNumber(phoneNumber);
        systemUser.setCreateBy("CreateUser");
        systemUser.setUpdateBy("CreateUser");
        Assert.isTrue(save(systemUser), "创建用户失败，请稍后重试");
        return systemUser;
    }

    @Override
    public void bindThirdUser(String systemUserId, Integer relationType, String oauthUserId) {
        Assert.isTrue(
                !systemUserRelationService.lambdaQuery()
                        .and(q -> q
                                .eq(SystemUserRelation::getRelationType, relationType)
                                .eq(SystemUserRelation::getSystemUserId, systemUserId))
                        .or(q -> q
                                .eq(SystemUserRelation::getRelationType, relationType)
                                .eq(SystemUserRelation::getRelationId, oauthUserId))
                        .exists(),
                "已绑定过或账号已被其他用户绑定，不能重复绑定");

        SystemUserRelation systemUserRelation = new SystemUserRelation();
        systemUserRelation.setRelationType(relationType);
        systemUserRelation.setRelationId(oauthUserId);
        systemUserRelation.setSystemUserId(systemUserId);
        systemUserRelation.setCreateBy("CreateUser");
        systemUserRelation.setUpdateBy("CreateUser");

        redisUtil.del("strix:system:user:userRelation::" + relationType + "-" + oauthUserId);

        systemUserRelationService.save(systemUserRelation);
    }

    @Cacheable(value = "strix:system:user:userRelation", key = "#relationType+'-'+#oauthUserId")
    @Override
    public SystemUser getSystemUser(Integer relationType, String oauthUserId) {
        SystemUserRelation systemUserRelation = systemUserRelationService.lambdaQuery()
                .eq(SystemUserRelation::getRelationType, relationType)
                .eq(SystemUserRelation::getRelationId, oauthUserId)
                .one();
        if (systemUserRelation != null) {
            return getBaseMapper().selectById(systemUserRelation.getSystemUserId());
        }
        return null;
    }

}
