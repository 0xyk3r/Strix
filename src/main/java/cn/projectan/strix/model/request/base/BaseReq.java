package cn.projectan.strix.model.request.base;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author 安炯奕
 * @date 2021/5/7 19:41
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BaseReq implements java.io.Serializable {

    private Boolean security = true;

}
