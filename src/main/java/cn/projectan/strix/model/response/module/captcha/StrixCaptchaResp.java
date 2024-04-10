package cn.projectan.strix.model.response.module.captcha;


import cn.projectan.strix.core.ret.RetCode;
import cn.projectan.strix.core.ret.RetResult;
import cn.projectan.strix.model.enums.CaptchaRepCodeEnum;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

@Data
public class StrixCaptchaResp implements Serializable {

    @Serial
    private static final long serialVersionUID = 8445617032523881407L;

    private String repCode;

    private String repMsg;

    private Object repData;

    public StrixCaptchaResp() {
        this.repCode = CaptchaRepCodeEnum.SUCCESS.getCode();
    }

    public static StrixCaptchaResp success() {
        return StrixCaptchaResp.successMsg("success");
    }

    public static StrixCaptchaResp successMsg(String message) {
        StrixCaptchaResp resp = new StrixCaptchaResp();
        resp.setRepMsg(message);
        return resp;
    }

    public static StrixCaptchaResp successData(Object data) {
        StrixCaptchaResp resp = new StrixCaptchaResp();
        resp.setRepCode(CaptchaRepCodeEnum.SUCCESS.getCode());
        resp.setRepData(data);
        return resp;
    }

    public static StrixCaptchaResp errorMsg(CaptchaRepCodeEnum message) {
        StrixCaptchaResp resp = new StrixCaptchaResp();
        resp.setRepCode(message.getCode());
        resp.setRepMsg(message.getDesc());
        return resp;
    }

    public static StrixCaptchaResp errorMsg(String message) {
        StrixCaptchaResp resp = new StrixCaptchaResp();
        resp.setRepCode(CaptchaRepCodeEnum.ERROR.getCode());
        resp.setRepMsg(message);
        return resp;
    }

    public static StrixCaptchaResp errorMsg(CaptchaRepCodeEnum captchaRepCodeEnum, String message) {
        StrixCaptchaResp resp = new StrixCaptchaResp();
        resp.setRepCode(captchaRepCodeEnum.getCode());
        resp.setRepMsg(message);
        return resp;
    }

    public static StrixCaptchaResp exceptionMsg(String message) {
        StrixCaptchaResp resp = new StrixCaptchaResp();
        resp.setRepCode(CaptchaRepCodeEnum.EXCEPTION.getCode());
        resp.setRepMsg(CaptchaRepCodeEnum.EXCEPTION.getDesc() + ": " + message);
        return resp;
    }

    public boolean isSuccess() {
        return CaptchaRepCodeEnum.SUCCESS.getCode().equals(repCode);
    }

    public RetResult<StrixCaptchaResp> toRetResult() {
        return new RetResult<>(RetCode.SUCCESS, repMsg, this);
    }

}
