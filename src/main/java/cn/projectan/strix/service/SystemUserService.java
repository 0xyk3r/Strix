package cn.projectan.strix.service;

import cn.projectan.strix.model.db.SystemUser;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * <p>
 * 服务类
 * </p>
 *
 * @author ProjectAn
 * @since 2021-08-26
 */
public interface SystemUserService extends IService<SystemUser> {

    /**
     * 创建系统用户
     *
     * @param nickname    用户昵称
     * @param phoneNumber 用户手机号码
     * @return 创建成功状态
     */
    SystemUser createSystemUser(String nickname, String phoneNumber);

    /**
     * 绑定第三方平台账号
     *
     * @param systemUserId 本系统用户id
     * @param relationType 都三方平台类型
     * @param oauthUserId   第三方平台用户id
     */
    void bindThirdUser(String systemUserId, Integer relationType, String oauthUserId);

    /**
     * 获取关联的SystemUser对象 带缓存
     *
     * @param relationType 关联类型
     * @param oauthUserId   关联id
     * @return SystemUser对象
     */
    SystemUser getSystemUser(Integer relationType, String oauthUserId);

}
