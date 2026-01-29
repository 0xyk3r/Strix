package cn.projectan.strix.service;

import cn.projectan.strix.mapper.system.OauthUserMapper;
import cn.projectan.strix.model.db.system.OauthUser;
import cn.projectan.strix.model.dict.system.OAuthPlatform;
import cn.projectan.strix.util.common.SpringUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 用于测试事务回滚的服务类
 * <p>
 * 对比使用 proxy 和不使用 proxy 在发生异常时的回滚情况
 * </p>
 *
 * @author ProjectAn
 */
@Service
public class TestTransactionService extends ServiceImpl<OauthUserMapper, OauthUser> {

    private static final Logger log = LoggerFactory.getLogger(TestTransactionService.class);

    /**
     * 使用 proxy.save() 后抛出异常
     */
    @Transactional(rollbackFor = Exception.class)
    public void saveWithProxyThenThrowException(String openId) {
        TestTransactionService proxy = SpringUtil.getAopProxy(this);

        OauthUser oauthUser = new OauthUser();
        oauthUser.setConfigId("1");
        oauthUser.setAppId("TEST_APP");
        oauthUser.setOpenId(openId);
        oauthUser.setPlatform(OAuthPlatform.WECHAT_OA);
        proxy.save(oauthUser);

        log.info("使用 proxy.save() 保存数据后，准备抛出异常...");
        throw new RuntimeException("测试异常 - 使用 proxy");
    }

    /**
     * 不使用 proxy，直接 save() 后抛出异常
     */
    @Transactional(rollbackFor = Exception.class)
    public void saveWithoutProxyThenThrowException(String openId) {
        OauthUser oauthUser = new OauthUser();
        oauthUser.setConfigId("1");
        oauthUser.setAppId("TEST_APP");
        oauthUser.setOpenId(openId);
        oauthUser.setPlatform(OAuthPlatform.WECHAT_OA);
        save(oauthUser);  // 直接调用，不使用 proxy

        log.info("直接使用 save() 保存数据后，准备抛出异常...");
        throw new RuntimeException("测试异常 - 不使用 proxy");
    }

    /**
     * 使用 proxy.save() 正常保存
     */
    @Transactional(rollbackFor = Exception.class)
    public void saveWithProxySuccess(String openId) {
        TestTransactionService proxy = SpringUtil.getAopProxy(this);

        OauthUser oauthUser = new OauthUser();
        oauthUser.setConfigId("1");
        oauthUser.setAppId("TEST_APP");
        oauthUser.setOpenId(openId);
        oauthUser.setPlatform(OAuthPlatform.WECHAT_OA);
        proxy.save(oauthUser);

        log.info("使用 proxy.save() 保存数据成功");
    }

    /**
     * 不使用 proxy，直接 save() 正常保存
     */
    @Transactional(rollbackFor = Exception.class)
    public void saveWithoutProxySuccess(String openId) {
        OauthUser oauthUser = new OauthUser();
        oauthUser.setConfigId("1");
        oauthUser.setAppId("TEST_APP");
        oauthUser.setOpenId(openId);
        oauthUser.setPlatform(OAuthPlatform.WECHAT_OA);
        save(oauthUser);  // 直接调用，不使用 proxy

        log.info("直接使用 save() 保存数据成功");
    }
}
