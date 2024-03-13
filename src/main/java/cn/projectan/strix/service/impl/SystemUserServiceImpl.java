package cn.projectan.strix.service.impl;

import cn.projectan.strix.mapper.SystemUserMapper;
import cn.projectan.strix.model.db.SystemUser;
import cn.projectan.strix.model.db.SystemUserRelation;
import cn.projectan.strix.model.dict.SystemUserStatus;
import cn.projectan.strix.service.SystemUserRelationService;
import cn.projectan.strix.service.SystemUserService;
import cn.projectan.strix.utils.RedisUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

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
        QueryWrapper<SystemUser> checkOneQueryWrapper = new QueryWrapper<>();
        checkOneQueryWrapper.eq("nickname", nickname);
        if (StringUtils.hasText(phoneNumber)) {
            checkOneQueryWrapper.or(qw -> qw.eq("phone_number", phoneNumber));
        }
        Assert.isTrue(getBaseMapper().selectCount(checkOneQueryWrapper) == 0, "昵称或手机号码已被使用，请更换后重试");

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
    public void bindThirdUser(String systemUserId, Integer relationType, String relationId) {
        QueryWrapper<SystemUserRelation> checkOneQueryWrapper = new QueryWrapper<>();
        checkOneQueryWrapper.eq("relation_type", relationType);
        checkOneQueryWrapper.eq("system_user_id", systemUserId);
        Assert.isTrue(systemUserRelationService.count(checkOneQueryWrapper) == 0, "已经绑定过该平台账号了，不能重复绑定");
        QueryWrapper<SystemUserRelation> checkTwoQueryWrapper = new QueryWrapper<>();
        checkTwoQueryWrapper.eq("relation_type", relationType);
        checkTwoQueryWrapper.eq("relation_id", relationId);
        Assert.isTrue(systemUserRelationService.count(checkTwoQueryWrapper) == 0, "该平台账号已被其他用户绑定，不能重复绑定");

        SystemUserRelation systemUserRelation = new SystemUserRelation();
        systemUserRelation.setRelationType(relationType);
        systemUserRelation.setRelationId(relationId);
        systemUserRelation.setSystemUserId(systemUserId);
        systemUserRelation.setCreateBy("CreateUser");
        systemUserRelation.setUpdateBy("CreateUser");

        redisUtil.del("strix:system:user:userRelation::" + relationType + "-" + relationId);

        systemUserRelationService.save(systemUserRelation);
    }

    @Cacheable(value = "strix:system:user:userRelation", key = "#relationType+'-'+#relationId")
    @Override
    public SystemUser getSystemUser(Integer relationType, String relationId) {
        QueryWrapper<SystemUserRelation> systemUserRelationQueryWrapper = new QueryWrapper<>();
        systemUserRelationQueryWrapper.eq("relation_type", relationType);
        systemUserRelationQueryWrapper.eq("relation_id", relationId);
        SystemUserRelation systemUserRelation = systemUserRelationService.getOne(systemUserRelationQueryWrapper);
        if (systemUserRelation != null) {
            return getBaseMapper().selectById(systemUserRelation.getSystemUserId());
        }

        return null;
    }

}
