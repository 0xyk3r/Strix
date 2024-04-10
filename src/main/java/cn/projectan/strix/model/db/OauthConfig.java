package cn.projectan.strix.model.db;

import cn.projectan.strix.model.db.base.BaseModel;
import com.baomidou.mybatisplus.annotation.TableField;
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
 * Strix OAuth 配置
 * </p>
 *
 * @author ProjectAn
 * @since 2024-04-03
 */
@Getter
@Setter
@Accessors(chain = true)
@NoArgsConstructor
@AllArgsConstructor
@TableName("sys_oauth_config")
public class OauthConfig extends BaseModel {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * OAuth 服务名称
     */
    @TableField("`name`")
    private String name;

    /**
     * OAuth 服务平台
     */
    private Integer platform;

    /**
     * 序列化后的配置信息
     */
    private String configData;

    public OauthConfig(String createBy, String updateBy) {
        super(createBy, updateBy);
    }

    public OauthConfig(LocalDateTime createTime, String createBy, LocalDateTime updateTime, String updateBy) {
        super(createTime, createBy, updateTime, updateBy);
    }

}
