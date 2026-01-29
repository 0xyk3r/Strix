package cn.projectan.strix.core.module.oauth;

import cn.projectan.strix.core.module.oauth.impl.WechatMPOAuthClient;
import cn.projectan.strix.model.dict.system.OAuthPlatform;
import cn.projectan.strix.model.other.system.module.oauth.BaseOAuthUserInfo;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * @author ProjectAn
 * @since 2026/1/29 10:15
 */
@Slf4j
@SpringBootTest
class WechatMPOAuthClientTest {

    @Autowired
    private StrixOAuthStore strixOAuthStore;

    @Test
    void testAuth() {
        WechatMPOAuthClient instance = (WechatMPOAuthClient) strixOAuthStore.getInstance("WechatMPTest", OAuthPlatform.WECHAT_MP);
        BaseOAuthUserInfo info = instance.auth("0f3779Ga1MDH5L0XMyFa1fVBaQ2779Gz");
        log.info("OAuth User Info: {}", info);
    }

    @Test
    void testGetPhoneNumber() {
        WechatMPOAuthClient instance = (WechatMPOAuthClient) strixOAuthStore.getInstance("WechatMPTest", OAuthPlatform.WECHAT_MP);
        String phoneNumber = instance.getPhoneNumber("b514fa2247e9a3ff73b7a5585a903614393b388892416da9635fe9fc2fd10a22");
        log.info("OAuth Phone Number: {}", phoneNumber);
    }

}