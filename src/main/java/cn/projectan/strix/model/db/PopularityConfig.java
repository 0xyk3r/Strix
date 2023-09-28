package cn.projectan.strix.model.db;

import cn.projectan.strix.model.db.base.BaseModel;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.io.Serial;
import java.time.LocalDateTime;

/**
 * <p>
 *
 * </p>
 *
 * @author 安炯奕
 * @since 2023-09-15
 */
@Getter
@Setter
@Accessors(chain = true)
@NoArgsConstructor
@AllArgsConstructor
@TableName("sys_popularity_config")
public class PopularityConfig extends BaseModel {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 数据类型
     */
    private String dataType;

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
    private Double magValue;

    public PopularityConfig(String createBy, String updateBy) {
        super(createBy, updateBy);
    }

    public PopularityConfig(LocalDateTime createTime, String createBy, LocalDateTime updateTime, String updateBy) {
        super(createTime, createBy, updateTime, updateBy);
    }

}
