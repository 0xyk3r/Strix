package cn.projectan.strix.model.db;

import cn.projectan.strix.model.db.base.BaseModel;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.io.Serial;

/**
 * <p>
 *
 * </p>
 *
 * @author ProjectAn
 * @since 2023-04-06
 */
@Getter
@Setter
@Accessors(chain = true)
@TableName("sys_security_url")
public class SecurityUrl extends BaseModel {

    @Serial
    private static final long serialVersionUID = 1L;

    private String url;

    private String ruleType;

    private String ruleValue;
}
