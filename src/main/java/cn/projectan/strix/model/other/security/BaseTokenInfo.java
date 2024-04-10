package cn.projectan.strix.model.other.security;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author ProjectAn
 * @date 2024/4/5 下午10:43
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BaseTokenInfo {

    private Integer uType;

    private String uid;

}
