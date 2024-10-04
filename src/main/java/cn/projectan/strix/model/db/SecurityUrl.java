package cn.projectan.strix.model.db;

import cn.projectan.strix.model.db.base.BaseModel;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.io.Serial;

/**
 * <p>
 * Strix Security URL配置
 * </p>
 *
 * @author ProjectAn
 * @since 2023-04-06
 */
@Getter
@Setter
@Accessors(chain = true)
@NoArgsConstructor
@AllArgsConstructor
@TableName("sys_security_url")
public class SecurityUrl extends BaseModel<SecurityUrl> {

    @Serial
    private static final long serialVersionUID = 1L;

    private String url;

    private String ruleType;

    private String ruleValue;

}
