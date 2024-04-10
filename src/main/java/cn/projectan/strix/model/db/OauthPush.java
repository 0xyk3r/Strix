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
 *
 * </p>
 *
 * @author ProjectAn
 * @since 2024-04-08
 */
@Getter
@Setter
@Accessors(chain = true)
@NoArgsConstructor
@AllArgsConstructor
@TableName("sys_oauth_push")
public class OauthPush extends BaseModel {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * OAuth 配置ID
     */
    private String configId;

    /**
     * OAuth APPID
     */
    private String appId;

    /**
     * 第三方用户OPENID
     */
    private String openId;

    /**
     * 推送模板内容
     */
    private String content;

    /**
     * 推送状态 1待推送 2推送成功 3推送失败
     */
    @TableField("`status`")
    private Byte status;

    /**
     * 第三方服务返回结果
     */
    private String result;

    public OauthPush(String createBy, String updateBy) {
        super(createBy, updateBy);
    }

    public OauthPush(LocalDateTime createTime, String createBy, LocalDateTime updateTime, String updateBy) {
        super(createTime, createBy, updateTime, updateBy);
    }

}
