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
 * @since 2021-08-26
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@TableName("tab_system_user")
public class SystemUser extends BaseModel {

    private static final long serialVersionUID = 1L;

    /**
     * 用户昵称
     */
    @UniqueDetection("用户昵称")
    private String nickname;

    /**
     * 0禁用 1启用
     */
    private Integer status;

    /**
     * 用户手机号码
     */
    @UniqueDetection("用户手机号码")
    private String phoneNumber;


}
