package cn.projectan.strix.model.db;

import cn.projectan.strix.model.annotation.UniqueDetection;
import cn.projectan.strix.model.db.base.BaseModel;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * <p>
 *
 * </p>
 *
 * @author 安炯奕
 * @since 2021-05-12
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@TableName("sys_system_permission")
public class SystemPermission extends BaseModel {

    private static final long serialVersionUID = 1L;

    /**
     * 权限名称
     */
    @UniqueDetection(value = "权限名称", group = 1)
    private String name;

    /**
     * 权限标识
     */
    @UniqueDetection(value = "权限标识", group = 2)
    private String permissionKey;

    /**
     * 1只读权限 2读写权限
     */
    @UniqueDetection(value = "权限类型", group = 1)
    @UniqueDetection(value = "权限类型", group = 2)
    private Integer permissionType;

    /**
     * 权限介绍
     */
    private String description;


}
