package cn.projectan.strix.model.other.system.captcha;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 验证码数据VO
 *
 * @author ProjectAn
 */
@Data
public class CaptchaDataVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 验证码类型 (blockPuzzle)
     */
    private String captchaType;

    /**
     * 加密密钥
     */
    private String secretKey;

    /**
     * 原生图片 base64
     */
    private String originalImageBase64;

    /**
     * 滑块图片base64
     */
    private String jigsawImageBase64;

    /**
     * 滑块点选坐标
     */
    private CaptchaPointVO point;

    /**
     * 点坐标(base64加密传输)
     */
    private String pointJson;

    /**
     * UUID (每次请求的验证码唯一标识)
     */
    private String token;

    /**
     * 校验结果
     */
    private Boolean result = false;

    /**
     * 后台二次校验参数
     */
    private String captchaVerification;

    /**
     * 客户端UI组件id,组件初始化时设置一次，UUID
     */
    private String clientUid;

    /**
     * 客户端ip+userAgent
     */
    private String browserInfo;

    public void resetClientFlag() {
        this.browserInfo = null;
        this.clientUid = null;
    }

}
