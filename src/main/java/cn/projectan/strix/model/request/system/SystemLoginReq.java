package cn.projectan.strix.model.request.system;

import cn.projectan.strix.model.request.base.BaseReq;
import lombok.Data;

/**
 * @author 安炯奕
 * @date 2021/5/12 18:42
 */
@Data
public class SystemLoginReq extends BaseReq {

    private String loginName;

    private String loginPassword;

}
