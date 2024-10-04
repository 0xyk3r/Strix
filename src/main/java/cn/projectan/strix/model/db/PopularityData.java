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
 * Strix 热度工具 数据
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
@TableName("sys_popularity_data")
public class PopularityData extends BaseModel<PopularityData> {

    @Serial
    private static final long serialVersionUID = 1L;

    private String configKey;

    private String dataId;

    private Long originalValue;

}
