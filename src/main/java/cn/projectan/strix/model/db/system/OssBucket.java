package cn.projectan.strix.model.db.system;

import cn.projectan.strix.model.db.base.BaseModel;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.io.Serial;

/**
 * <p>
 * Strix OSS 容器
 * </p>
 *
 * @author ProjectAn
 * @since 2023-05-23
 */
@Getter
@Setter
@Accessors(chain = true)
@NoArgsConstructor
@AllArgsConstructor
@TableName("sys_oss_bucket")
public class OssBucket extends BaseModel<OssBucket> {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * OSS 配置 Key
     */
    private String configKey;

    /**
     * OSS Bucket 名称
     */
    @TableField("`name`")
    private String name;

    /**
     * 备注
     */
    private String remark;

}
