package cn.projectan.strix.model.db;

import cn.projectan.strix.model.annotation.UniqueDetection;
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
 * Strix 系统地区
 * </p>
 *
 * @author ProjectAn
 * @since 2021-09-29
 */
@Getter
@Setter
@Accessors(chain = true)
@NoArgsConstructor
@AllArgsConstructor
@TableName("sys_system_region")
public class SystemRegion extends BaseModel<SystemRegion> {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 地区名称
     */
    @UniqueDetection(value = "地区名称", group = 1)
    private String name;

    /**
     * 地区等级 从1开始 从小到大
     */
    private Integer level;

    /**
     * 父节点id
     */
    @UniqueDetection(value = "父级地区", group = 1)
    private String parentId;

    /**
     * 完整节点路径，以英文逗号开头、结尾、连接
     */
    private String fullPath;

    /**
     * 完整地区地址信息
     */
    private String fullName;

    /**
     * 备注信息
     */
    private String remarks;

}
