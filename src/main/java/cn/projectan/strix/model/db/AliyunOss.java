package cn.projectan.strix.model.db;

import cn.projectan.strix.model.db.base.BaseModel;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;

/**
 * <p>
 * 阿里云OSS配置
 * </p>
 *
 * @author 安炯奕
 * @since 2021-05-02
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_aliyun_oss")
public class AliyunOss extends BaseModel {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 公网连接域名
     */
    private String publicEndpoint;

    /**
     * 私网连接域名
     */
    private String privateEndpoint;

    /**
     * 授权令牌ID
     */
    private String accessKeyId;

    /**
     * 授权令牌秘钥
     */
    private String accessKeySecret;


}
