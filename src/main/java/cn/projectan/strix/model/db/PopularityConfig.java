package cn.projectan.strix.model.db;

import cn.projectan.strix.model.annotation.UniqueField;
import cn.projectan.strix.model.db.base.BaseModel;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.io.Serial;
import java.math.BigDecimal;

/**
 * <p>
 * Strix 热度工具 配置
 * </p>
 *
 * @author ProjectAn
 * @since 2023-09-15
 */
@Getter
@Setter
@Accessors(chain = true)
@NoArgsConstructor
@AllArgsConstructor
@TableName("sys_popularity_config")
public class PopularityConfig extends BaseModel<PopularityConfig> {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 配置名称
     */
    @UniqueField("配置名称")
    private String name;

    /**
     * 配置 Key
     */
    @UniqueField("配置Key")
    private String configKey;

    /**
     * 初始的数值（参与乘算）
     */
    private Integer initialValue;

    /**
     * 附加的额外数值（不参与乘算）
     */
    private Integer extraValue;

    /**
     * 数值倍率
     */
    private BigDecimal magValue;

}
