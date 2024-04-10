package cn.projectan.strix.model.properties;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @author ProjectAn
 * @date 2024/4/5 下午6:30
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "strix.security.jwt")
public class StrixJwtProperties {

    /**
     * 密钥
     */
    private String secretKey;

    /**
     * Token 过期时间
     */
    private Long expireTime;

    /**
     * 刷新Token 过期时间
     */
    private Long refreshExpireTime;

}
