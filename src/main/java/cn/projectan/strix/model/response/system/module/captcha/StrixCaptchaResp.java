package cn.projectan.strix.model.response.system.module.captcha;


import cn.projectan.strix.core.ret.RetCode;
import cn.projectan.strix.core.ret.RetResult;
import cn.projectan.strix.model.enums.system.StrixCaptchaRepCodeEnum;
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
        this.repCode = StrixCaptchaRepCodeEnum.SUCCESS.getCode();
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
        resp.setRepCode(StrixCaptchaRepCodeEnum.SUCCESS.getCode());
        resp.setRepData(data);
        return resp;
    }

    public static StrixCaptchaResp errorMsg(StrixCaptchaRepCodeEnum message) {
        StrixCaptchaResp resp = new StrixCaptchaResp();
        resp.setRepCode(message.getCode());
        resp.setRepMsg(message.getDesc());
        return resp;
    }

    public static StrixCaptchaResp errorMsg(String message) {
        StrixCaptchaResp resp = new StrixCaptchaResp();
        resp.setRepCode(StrixCaptchaRepCodeEnum.ERROR.getCode());
        resp.setRepMsg(message);
        return resp;
    }

    public static StrixCaptchaResp errorMsg(StrixCaptchaRepCodeEnum strixCaptchaRepCodeEnum, String message) {
        StrixCaptchaResp resp = new StrixCaptchaResp();
        resp.setRepCode(strixCaptchaRepCodeEnum.getCode());
        resp.setRepMsg(message);
        return resp;
    }

    public static StrixCaptchaResp exceptionMsg(String message) {
        StrixCaptchaResp resp = new StrixCaptchaResp();
        resp.setRepCode(StrixCaptchaRepCodeEnum.EXCEPTION.getCode());
        resp.setRepMsg(StrixCaptchaRepCodeEnum.EXCEPTION.getDesc() + ": " + message);
        return resp;
    }

    public boolean isSuccess() {
        return StrixCaptchaRepCodeEnum.SUCCESS.getCode().equals(repCode);
    }

    public RetResult<StrixCaptchaResp> toRetResult() {
        return new RetResult<>(RetCode.SUCCESS, repMsg, this);
    }

}
