package cn.projectan.strix.service;

import cn.projectan.strix.mapper.system.OauthUserMapper;
import cn.projectan.strix.model.db.system.OauthUser;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 测试使用 proxy 和不使用 proxy 在事务中发生异常时的回滚情况
 * <p>
 * 结论：在 @Transactional 标注的方法内部，无论使用 proxy.save() 还是 this.save()，
 * 发生异常时都会正常回滚，因为事务边界是在方法入口处开启的。
 * </p>
 *
 * @author ProjectAn
 */
@Slf4j
@SpringBootTest
class TransactionProxyTest {

    @Autowired
    private TestTransactionService testTransactionService;

    @Autowired
    private OauthUserMapper oauthUserMapper;

    private static final String TEST_OPEN_ID_WITH_PROXY = "TEST_PROXY_OPEN_ID";
    private static final String TEST_OPEN_ID_WITHOUT_PROXY = "TEST_NO_PROXY_OPEN_ID";

    @BeforeEach
    void setUp() {
        // 清理测试数据
        oauthUserMapper.delete(new LambdaQueryWrapper<OauthUser>()
                .eq(OauthUser::getOpenId, TEST_OPEN_ID_WITH_PROXY));
        oauthUserMapper.delete(new LambdaQueryWrapper<OauthUser>()
                .eq(OauthUser::getOpenId, TEST_OPEN_ID_WITHOUT_PROXY));
    }

    @Test
    @DisplayName("测试使用 proxy.save() - 发生异常时应该回滚")
    void testWithProxy_shouldRollbackOnException() {
        // 验证初始状态：数据不存在
        OauthUser beforeInsert = oauthUserMapper.selectOne(
                new LambdaQueryWrapper<OauthUser>().eq(OauthUser::getOpenId, TEST_OPEN_ID_WITH_PROXY));
        assertNull(beforeInsert, "测试前数据应该不存在");

        // 执行会抛出异常的方法
        assertThrows(RuntimeException.class, () ->
                testTransactionService.saveWithProxyThenThrowException(TEST_OPEN_ID_WITH_PROXY));

        // 验证数据已回滚（不存在）
        OauthUser afterException = oauthUserMapper.selectOne(
                new LambdaQueryWrapper<OauthUser>().eq(OauthUser::getOpenId, TEST_OPEN_ID_WITH_PROXY));
        assertNull(afterException, "使用 proxy.save() 后发生异常，数据应该回滚");

        log.info("✅ 使用 proxy.save() 发生异常时，事务正常回滚");
    }

    @Test
    @DisplayName("测试不使用 proxy，直接 save() - 发生异常时应该回滚")
    void testWithoutProxy_shouldRollbackOnException() {
        // 验证初始状态：数据不存在
        OauthUser beforeInsert = oauthUserMapper.selectOne(
                new LambdaQueryWrapper<OauthUser>().eq(OauthUser::getOpenId, TEST_OPEN_ID_WITHOUT_PROXY));
        assertNull(beforeInsert, "测试前数据应该不存在");

        // 执行会抛出异常的方法
        assertThrows(RuntimeException.class, () ->
                testTransactionService.saveWithoutProxyThenThrowException(TEST_OPEN_ID_WITHOUT_PROXY));

        // 验证数据已回滚（不存在）
        OauthUser afterException = oauthUserMapper.selectOne(
                new LambdaQueryWrapper<OauthUser>().eq(OauthUser::getOpenId, TEST_OPEN_ID_WITHOUT_PROXY));
        assertNull(afterException, "直接使用 save() 后发生异常，数据应该回滚");

        log.info("✅ 直接使用 save() 发生异常时，事务正常回滚");
    }

    @Test
    @DisplayName("测试使用 proxy.save() - 正常执行时数据应该保存成功")
    void testWithProxy_shouldCommitOnSuccess() {
        // 验证初始状态：数据不存在
        OauthUser beforeInsert = oauthUserMapper.selectOne(
                new LambdaQueryWrapper<OauthUser>().eq(OauthUser::getOpenId, TEST_OPEN_ID_WITH_PROXY));
        assertNull(beforeInsert, "测试前数据应该不存在");

        // 执行正常的保存方法
        testTransactionService.saveWithProxySuccess(TEST_OPEN_ID_WITH_PROXY);

        // 验证数据已保存
        OauthUser afterSave = oauthUserMapper.selectOne(
                new LambdaQueryWrapper<OauthUser>().eq(OauthUser::getOpenId, TEST_OPEN_ID_WITH_PROXY));
        assertNotNull(afterSave, "使用 proxy.save() 正常执行，数据应该保存成功");

        log.info("✅ 使用 proxy.save() 正常执行时，数据保存成功");
    }

    @Test
    @DisplayName("测试不使用 proxy，直接 save() - 正常执行时数据应该保存成功")
    void testWithoutProxy_shouldCommitOnSuccess() {
        // 验证初始状态：数据不存在
        OauthUser beforeInsert = oauthUserMapper.selectOne(
                new LambdaQueryWrapper<OauthUser>().eq(OauthUser::getOpenId, TEST_OPEN_ID_WITHOUT_PROXY));
        assertNull(beforeInsert, "测试前数据应该不存在");

        // 执行正常的保存方法
        testTransactionService.saveWithoutProxySuccess(TEST_OPEN_ID_WITHOUT_PROXY);

        // 验证数据已保存
        OauthUser afterSave = oauthUserMapper.selectOne(
                new LambdaQueryWrapper<OauthUser>().eq(OauthUser::getOpenId, TEST_OPEN_ID_WITHOUT_PROXY));
        assertNotNull(afterSave, "直接使用 save() 正常执行，数据应该保存成功");

        log.info("✅ 直接使用 save() 正常执行时，数据保存成功");
    }
}
