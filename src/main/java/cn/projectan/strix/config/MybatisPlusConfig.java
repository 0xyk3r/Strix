package cn.projectan.strix.config;

import cn.projectan.strix.utils.SecurityUtils;
import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.OptimisticLockerInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import org.apache.ibatis.reflection.MetaObject;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * MybatisPlus 配置类
 *
 * @author 安炯奕
 * @date 2021/05/02 17:53
 */
@Configuration
@EnableTransactionManagement
@MapperScan({"cn.projectan.**.mapper"})
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
     * MyBatisPlus 字段自动填充配置
     *
     * @author 安炯奕
     * @date 2022/7/15 18:42
     */
    @Component
    public static class MybatisMetaObjectConfig implements MetaObjectHandler {

        @Override
        public void insertFill(MetaObject metaObject) {
            this.strictInsertFill(metaObject, "createTime", LocalDateTime.class, LocalDateTime.now());
            this.strictInsertFill(metaObject, "updateTime", LocalDateTime.class, LocalDateTime.now());
            this.strictInsertFill(metaObject, "deletedStatus", Integer.class, 0);

            String loginManagerId = Optional.ofNullable(SecurityUtils.getManagerId()).orElse("0");
            this.strictInsertFill(metaObject, "createBy", String.class, loginManagerId);
            this.strictInsertFill(metaObject, "updateBy", String.class, loginManagerId);
        }

        @Override
        public void updateFill(MetaObject metaObject) {
            // strictUpdateFill 在原对象有值时，不会覆盖原值，所以这里使用 setFieldValByName
            this.setFieldValByName("updateTime", LocalDateTime.now(), metaObject);

            String loginManagerId = Optional.ofNullable(SecurityUtils.getManagerId()).orElse("0");
            this.setFieldValByName("updateBy", loginManagerId, metaObject);
        }

    }

}
