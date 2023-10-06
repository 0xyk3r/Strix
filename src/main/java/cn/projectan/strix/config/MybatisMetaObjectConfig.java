package cn.projectan.strix.config;

import cn.projectan.strix.utils.SecurityUtils;
import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * MyBatisPlus 字段自动填充配置
 *
 * @author 安炯奕
 * @date 2022/7/15 18:42
 */
@Component
public class MybatisMetaObjectConfig implements MetaObjectHandler {

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
        this.strictUpdateFill(metaObject, "updateTime", LocalDateTime.class, LocalDateTime.now());

        String loginManagerId = Optional.ofNullable(SecurityUtils.getManagerId()).orElse("0");
        this.strictInsertFill(metaObject, "updateBy", String.class, loginManagerId);
    }

}
