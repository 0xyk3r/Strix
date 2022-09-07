package cn.projectan.strix.model.db;

import com.baomidou.mybatisplus.annotation.TableName;
import cn.projectan.strix.model.db.base.BaseModel;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * <p>
 * 
 * </p>
 *
 * @author 安炯奕
 * @since 2021-08-26
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("tab_system_user_relation")
public class SystemUserRelation extends BaseModel {

    private static final long serialVersionUID = 1L;

    /**
     * 关系类型 1微信
     */
    private Integer relationType;

    /**
     * 所关联的第三方用户id
     */
    private String relationId;

    /**
     * 系统内用户id
     */
    private String systemUserId;


}
