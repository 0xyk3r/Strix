package cn.projectan.strix.service.system;

import cn.projectan.strix.core.ss.details.LoginSystemUser;
import cn.projectan.strix.mapper.system.SystemUserMapper;
import cn.projectan.strix.model.constant.system.OperatorType;
import cn.projectan.strix.model.db.system.SystemUser;
import cn.projectan.strix.model.db.system.SystemUserRelation;
import cn.projectan.strix.model.dict.system.SystemUserStatus;
import cn.projectan.strix.model.enums.common.NumCategory;
import cn.projectan.strix.model.request.system.user.SystemUserListReq;
import cn.projectan.strix.util.common.RedisUtil;
import cn.projectan.strix.util.math.NumUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * <p>
 * Strix 系统用户 服务类
 * </p>
 *
 * @author ProjectAn
 * @since 2021-08-26
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SystemUserService extends ServiceImpl<SystemUserMapper, SystemUser> {

    private final SystemUserRelationService systemUserRelationService;
    private final RedisUtil redisUtil;

    /**
     * 分页查询系统用户列表
     *
     * @param req 查询请求
     * @return 分页数据
     */
    public Page<SystemUser> listPage(SystemUserListReq req) {
        return lambdaQuery()
                .like(StringUtils.hasText(req.getKeyword()), SystemUser::getNickname, req.getKeyword())
                .or(StringUtils.hasText(req.getKeyword()), q -> q.like(SystemUser::getPhoneNumber, req.getKeyword()))
                .eq(NumUtil.checkCategory(req.getStatus(), NumCategory.NON_NEGATIVE), SystemUser::getStatus, req.getStatus())
                .orderByAsc(SystemUser::getCreatedTime)
                .page(req.getPage());
    }

    /**
     * 创建系统用户
     *
     * @param nickname    用户昵称
     * @param phoneNumber 用户手机号码
     * @return 创建成功状态
     */
    public SystemUser createSystemUser(String nickname, String phoneNumber) {
        Assert.isTrue(
                !lambdaQuery()
                        .eq(SystemUser::getNickname, nickname)
                        .or(q -> q.eq(SystemUser::getPhoneNumber, phoneNumber))
                        .exists(),
                "昵称或手机号码已被使用，请更换后重试");

        SystemUser systemUser = new SystemUser()
                .setNickname(nickname)
                .setStatus(SystemUserStatus.NORMAL)
                .setPhoneNumber(phoneNumber)
                .setCreatedByType(OperatorType.SYSTEM)
                .setUpdatedByType(OperatorType.SYSTEM);
        Assert.isTrue(save(systemUser), "创建用户失败，请稍后重试");
        return systemUser;
    }

    /**
     * 绑定第三方平台账号
     *
     * @param systemUserId 本系统用户id
     * @param relationType 都三方平台类型
     * @param oauthUserId  第三方平台用户id
     */
    public void bindThirdUser(String systemUserId, Short relationType, String oauthUserId) {
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

        SystemUserRelation systemUserRelation = new SystemUserRelation()
                .setRelationType(relationType)
                .setRelationId(oauthUserId)
                .setSystemUserId(systemUserId)
                .setCreatedByType(OperatorType.SYSTEM)
                .setUpdatedByType(OperatorType.SYSTEM);

        redisUtil.del("strix:system:user:userRelation::" + relationType + "-" + oauthUserId);
        systemUserRelationService.save(systemUserRelation);
    }

    /**
     * 获取关联的SystemUser对象 带缓存
     *
     * @param relationType 关联类型
     * @param oauthUserId  关联id
     * @return SystemUser对象
     */
    @Cacheable(value = "strix:system:user:userRelation", key = "#relationType+'-'+#oauthUserId")
    public SystemUser getSystemUser(Short relationType, String oauthUserId) {
        SystemUserRelation systemUserRelation = systemUserRelationService.lambdaQuery()
                .eq(SystemUserRelation::getRelationType, relationType)
                .eq(SystemUserRelation::getRelationId, oauthUserId)
                .one();
        if (systemUserRelation != null) {
            return getBaseMapper().selectById(systemUserRelation.getSystemUserId());
        }
        return null;
    }

    /**
     * 删除用户及其关联的第三方账号绑定
     *
     * @param systemUser 待删除的用户
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteUserWithRelations(SystemUser systemUser) {
        removeById(systemUser);
        systemUserRelationService.lambdaUpdate()
                .eq(SystemUserRelation::getSystemUserId, systemUser.getId())
                .remove();
    }

    /**
     * 登陆时获取用户完整权限信息
     *
     * @param userId 用户 ID
     * @return 登陆用户信息
     */
    public LoginSystemUser getLoginInfo(String userId) {
        SystemUser systemUser = getById(userId);
        return new LoginSystemUser(systemUser);
    }

}
