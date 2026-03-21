package cn.projectan.strix.model.other.system.security;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author ProjectAn
 * @since 2024/4/5 下午10:38
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TokenInfo {

    private String token;

    private String refreshToken;

}
