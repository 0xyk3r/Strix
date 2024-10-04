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
 * Strix 系统用户 第三方账户绑定关系
 * </p>
 *
 * @author ProjectAn
 * @since 2021-08-26
 */
@Getter
@Setter
@Accessors(chain = true)
@NoArgsConstructor
@AllArgsConstructor
@TableName("sys_system_user_relation")
public class SystemUserRelation extends BaseModel<SystemUserRelation> {

    @Serial
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
