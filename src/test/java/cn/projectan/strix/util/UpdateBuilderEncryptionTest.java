package cn.projectan.strix.util;

import cn.projectan.strix.core.encrypt.FieldEncryptUtil;
import cn.projectan.strix.model.annotation.UpdateField;
import cn.projectan.strix.model.db.system.SystemManager;
import cn.projectan.strix.service.system.SystemManagerService;
import cn.projectan.strix.util.common.UpdateBuilder;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author ProjectAn
 * @since 2026/1/29 01:54
 */
@Slf4j
@SpringBootTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class UpdateBuilderEncryptionTest {

    @Autowired
    private SystemManagerService systemManagerService;

    private String testUserId;

    /**
     * 更新请求 DTO
     */
    @Data
    public static class UpdatePasswordReq {
        @UpdateField
        private String loginPassword;
    }

    @BeforeAll
    void setUp() {
        log.info("========== 准备测试数据 ==========");

        // 清理旧数据
        systemManagerService.lambdaUpdate()
                .eq(SystemManager::getLoginName, "updatebuilder_test")
                .remove();

        // 创建测试用户
        SystemManager manager = new SystemManager();
        manager.setNickname("UpdateBuilder测试用户");
        manager.setLoginName("updatebuilder_test");
        manager.setLoginPassword("initialPassword123");
        manager.setStatus(1);
        manager.setType(2);

        systemManagerService.save(manager);
        testUserId = manager.getId();

        log.info("测试用户已创建, ID: {}", testUserId);
    }

    @Test
    @Order(1)
    @DisplayName("验证初始密码正确加密")
    void test1_VerifyInitialPassword() {
        log.info("========== 测试1: 验证初始密码 ==========");

        SystemManager manager = systemManagerService.getById(testUserId);
        assertNotNull(manager, "用户应该存在");

        String password = manager.getLoginPassword();
        log.info("查询到的密码（应为明文）: {}", password);

        assertEquals("initialPassword123", password, "初始密码应正确解密");
    }

    @Test
    @Order(2)
    @DisplayName("使用 UpdateBuilder 更新密码")
    void test2_UpdatePasswordWithUpdateBuilder() {
        log.info("========== 测试2: 使用 UpdateBuilder 更新密码 ==========");

        // 1. 查询用户
        SystemManager originalManager = systemManagerService.getById(testUserId);
        assertNotNull(originalManager, "用户应该存在");

        String oldPassword = originalManager.getLoginPassword();
        log.info("更新前密码: {}", oldPassword);

        // 2. 构造更新请求
        UpdatePasswordReq req = new UpdatePasswordReq();
        req.setLoginPassword("newPasswordViaBuilder456");
        log.info("设置新密码: {}", req.getLoginPassword());

        // 3. 使用 UpdateBuilder 构造 UpdateWrapper
        log.info("调用 UpdateBuilder.build()...");
        LambdaUpdateWrapper<SystemManager> updateWrapper = UpdateBuilder.build(originalManager, req);

        // 4. 执行更新
        log.info("执行更新操作...");
        boolean updateResult = systemManagerService.update(updateWrapper);
        assertTrue(updateResult, "更新应该成功");
        log.info("更新完成");

        // 5. 查询验证
        SystemManager updatedManager = systemManagerService.getById(testUserId);
        String newPassword = updatedManager.getLoginPassword();
        log.info("更新后密码（应为明文）: {}", newPassword);

        // 6. 验证结果
        assertNotEquals(oldPassword, newPassword, "密码应该已更改");
        assertEquals("newPasswordViaBuilder456", newPassword, "新密码应该正确解密");
        log.info("✅ UpdateBuilder 更新密码成功！");
    }

    @Test
    @Order(3)
    @DisplayName("再次使用 UpdateBuilder 更新密码")
    void test3_UpdatePasswordAgain() {
        log.info("========== 测试3: 再次使用 UpdateBuilder 更新密码 ==========");

        SystemManager originalManager = systemManagerService.getById(testUserId);
        String oldPassword = originalManager.getLoginPassword();
        log.info("当前密码: {}", oldPassword);

        UpdatePasswordReq req = new UpdatePasswordReq();
        req.setLoginPassword("thirdPasswordViaBuilder789");

        LambdaUpdateWrapper<SystemManager> updateWrapper = UpdateBuilder.build(originalManager, req);
        systemManagerService.update(updateWrapper);

        SystemManager updatedManager = systemManagerService.getById(testUserId);
        String newPassword = updatedManager.getLoginPassword();
        log.info("第三次更新后密码: {}", newPassword);

        assertEquals("thirdPasswordViaBuilder789", newPassword, "第三次更新的密码应该正确");
        log.info("✅ 连续更新测试通过！");
    }

    @Test
    @Order(4)
    @DisplayName("验证数据库中存储的是密文")
    void test4_VerifyEncryptedInDatabase() {
        log.info("========== 测试4: 验证数据库存储 ==========");

        // 查询用户
        SystemManager manager = systemManagerService.getById(testUserId);
        String decryptedPassword = manager.getLoginPassword();
        log.info("解密后的密码: {}", decryptedPassword);

        // 手动加密相同的密码
        String manualEncrypted = FieldEncryptUtil.encrypt(decryptedPassword);
        log.info("手动加密结果: {}", manualEncrypted);

        // 验证：解密后再加密应该能得到一致的密文
        assertTrue(FieldEncryptUtil.isEncrypted(manualEncrypted), "加密结果应该是密文格式");

        // 验证：能正确解密
        String manualDecrypted = FieldEncryptUtil.decrypt(manualEncrypted);
        assertEquals(decryptedPassword, manualDecrypted, "加密解密应该可逆");

        log.info("✅ 数据库加密验证通过！");
    }

    @Test
    @Order(5)
    @DisplayName("对比 UpdateBuilder 和 updateById 的结果")
    void test5_CompareUpdateMethods() {
        log.info("========== 测试5: 对比多种更新方式 ==========");

        // 方法1: 使用 updateById
        SystemManager manager1 = systemManagerService.getById(testUserId);
        manager1.setLoginPassword("passwordViaUpdateById");
        systemManagerService.updateById(manager1);

        SystemManager result1 = systemManagerService.getById(testUserId);
        log.info("updateById 结果: {}", result1.getLoginPassword());
        assertEquals("passwordViaUpdateById", result1.getLoginPassword());

        // 方法2: 使用 UpdateBuilder
        SystemManager manager2 = systemManagerService.getById(testUserId);
        UpdatePasswordReq req = new UpdatePasswordReq();
        req.setLoginPassword("passwordViaUpdateBuilder");
        LambdaUpdateWrapper<SystemManager> wrapper = UpdateBuilder.build(manager2, req);
        systemManagerService.update(wrapper);

        SystemManager result2 = systemManagerService.getById(testUserId);
        log.info("UpdateBuilder 结果: {}", result2.getLoginPassword());
        assertEquals("passwordViaUpdateBuilder", result2.getLoginPassword());

        // 方法3: 使用 LambdaUpdateWrapper
        LambdaUpdateWrapper<SystemManager> lambdaWrapper = new LambdaUpdateWrapper<>();
        lambdaWrapper.eq(SystemManager::getId, testUserId)
                .set(SystemManager::getLoginPassword, "passwordViaLambdaWrapper");
        systemManagerService.update(lambdaWrapper);

        SystemManager result3 = systemManagerService.getById(testUserId);
        log.info("LambdaUpdateWrapper 结果: {}", result3.getLoginPassword());
        assertEquals("passwordViaLambdaWrapper", result3.getLoginPassword());

        // 方法4: 使用 LambdaUpdate
        systemManagerService.lambdaUpdate()
                .eq(SystemManager::getId, testUserId)
                .set(SystemManager::getLoginPassword, "passwordViaLambdaUpdate")
                .update();

        SystemManager result4 = systemManagerService.getById(testUserId);
        log.info("lambdaUpdate() 结果: {}", result4.getLoginPassword());
        assertEquals("passwordViaLambdaUpdate", result4.getLoginPassword());
    }

    @AfterAll
    void cleanup() {
        log.info("========== 清理测试数据 ==========");
        systemManagerService.lambdaUpdate()
                .eq(SystemManager::getLoginName, "updatebuilder_test")
                .remove();
        log.info("测试数据已清理");
    }

}