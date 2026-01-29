package cn.projectan.strix.config;

import cn.projectan.strix.core.encrypt.FieldEncryptionInterceptor;
import cn.projectan.strix.util.system.SecurityUtils;
import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.OptimisticLockerInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * MybatisPlus 配置类
 *
 * @author ProjectAn
 * @since 2021/05/02 17:53
 */
@Configuration
@EnableTransactionManagement
//@MapperScan({"cn.projectan.**.mapper"})
public class MybatisPlusConfig {

    /**
     * MybatisPlus 插件/拦截器 配置
     */
    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();

        // 分页插件
        PaginationInnerInterceptor paginationInnerInterceptor = new PaginationInnerInterceptor(DbType.MYSQL);
        paginationInnerInterceptor.setMaxLimit(1000L);
        interceptor.addInnerInterceptor(paginationInnerInterceptor);

        // 乐观锁插件
        OptimisticLockerInnerInterceptor optimisticLockerInnerInterceptor = new OptimisticLockerInnerInterceptor();
        interceptor.addInnerInterceptor(optimisticLockerInnerInterceptor);

        return interceptor;
    }

    /**
     * 字段加密解密拦截器
     * <p>
     * 自动对带有 @EncryptField 注解的字段进行加密解密
     */
    @Bean
    public FieldEncryptionInterceptor fieldEncryptionInterceptor() {
        return new FieldEncryptionInterceptor();
    }

    /**
     * MyBatisPlus 字段自动填充配置
     *
     * @author ProjectAn
     * @since 2022/7/15 18:42
     */
    @Component
    public static class MybatisMetaObjectConfig implements MetaObjectHandler {

        @Override
        public void insertFill(MetaObject metaObject) {
            this.strictInsertFill(metaObject, "createdTime", LocalDateTime.class, LocalDateTime.now());
            this.strictInsertFill(metaObject, "updatedTime", LocalDateTime.class, LocalDateTime.now());
            this.strictInsertFill(metaObject, "deletedStatus", Short.class, (short) 0);

            short operatorType = SecurityUtils.getOperatorType();
            String operatorId = Optional.ofNullable(SecurityUtils.getOperatorId()).orElse("0");
            this.strictInsertFill(metaObject, "createdByType", Short.class, operatorType);
            this.strictInsertFill(metaObject, "updatedByType", Short.class, operatorType);
            this.strictInsertFill(metaObject, "createdBy", String.class, operatorId);
            this.strictInsertFill(metaObject, "updatedBy", String.class, operatorId);
        }

        @Override
        public void updateFill(MetaObject metaObject) {
            // strictUpdateFill 在原对象有值时，不会覆盖原值，所以这里使用 setFieldValByName
            this.setFieldValByName("updatedTime", LocalDateTime.now(), metaObject);

            short operatorType = SecurityUtils.getOperatorType();
            String operatorId = Optional.ofNullable(SecurityUtils.getOperatorId()).orElse("0");
            this.setFieldValByName("updatedByType", operatorType, metaObject);
            this.setFieldValByName("updatedBy", operatorId, metaObject);
        }

    }

}
