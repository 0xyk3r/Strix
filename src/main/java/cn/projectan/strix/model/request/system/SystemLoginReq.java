package cn.projectan.strix.model.request.system;

import cn.projectan.strix.core.datamask.DataMask;
import lombok.Data;

/**
 * @author 安炯奕
 * @date 2021/5/12 18:42
 */
@Data
public class SystemLoginReq {

    private String loginName;

    @DataMask
    private String loginPassword;

    private String captchaVerification;

}
