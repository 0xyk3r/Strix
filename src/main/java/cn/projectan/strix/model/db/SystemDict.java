package cn.projectan.strix.model.db;

import cn.projectan.strix.model.db.base.BaseModel;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;

/**
 * <p>
 *
 * </p>
 *
 * @author 安炯奕
 * @since 2021-08-31
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_system_dict")
public class SystemDict extends BaseModel {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 字典key
     */
    private String dictKey;

    /**
     * 字典值
     */
    private String dictValue;


}
