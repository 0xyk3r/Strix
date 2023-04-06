package cn.projectan.strix.model.db;

import cn.projectan.strix.model.db.base.BaseModel;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

/**
 * <p>
 *
 * </p>
 *
 * @author 安炯奕
 * @since 2023-04-06
 */
@Getter
@Setter
@Accessors(chain = true)
@TableName("tab_security_url")
public class SecurityUrl extends BaseModel {

    private static final long serialVersionUID = 1L;

    private String url;

    private String ruleType;

    private String ruleValue;
}
