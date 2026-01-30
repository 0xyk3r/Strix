package cn.projectan.strix.controller.system.common;

import cn.projectan.strix.controller.system.base.BaseSystemController;
import cn.projectan.strix.core.captcha.CaptchaService;
import cn.projectan.strix.core.ret.RetResult;
import cn.projectan.strix.model.annotation.Anonymous;
import cn.projectan.strix.model.other.system.captcha.StrixCaptchaInfoVO;
import cn.projectan.strix.model.request.system.module.captcha.CheckCaptchaReq;
import cn.projectan.strix.model.request.system.module.captcha.GetCaptchaReq;
import cn.projectan.strix.model.response.system.module.captcha.StrixCaptchaResp;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 系统验证码
 *
 * @author ProjectAn
 * @since 2024/03/31 01:35:00
 */
@Anonymous
@RestController("SystemCaptchaController")
@RequestMapping("system/captcha")
@RequiredArgsConstructor
@Tag(name = "通用 - 验证码")
public class CaptchaController extends BaseSystemController {

    private final CaptchaService captchaService;

    /**
     * 获取验证码
     */
    @Anonymous
    @PostMapping("get")
    @Operation(summary = "获取验证码")
    public RetResult<StrixCaptchaResp> get(@RequestBody GetCaptchaReq req, HttpServletRequest request) {
        Assert.notNull(request.getRemoteHost(), "请求无效");
        StrixCaptchaInfoVO data = new StrixCaptchaInfoVO();
        data.setCaptchaType(req.getCaptchaType());
        data.setBrowserInfo(getRemoteId(request));
        return captchaService.get(data).toRetResult();
    }

    /**
     * 校验验证码
     */
    @Anonymous
    @PostMapping("check")
    @Operation(summary = "校验验证码")
    public RetResult<StrixCaptchaResp> check(@RequestBody CheckCaptchaReq req, HttpServletRequest request) {
        StrixCaptchaInfoVO data = new StrixCaptchaInfoVO();
        data.setCaptchaType(req.getCaptchaType());
        data.setPointJson(req.getPointJson());
        data.setToken(req.getToken());
        data.setBrowserInfo(getRemoteId(request));
        return captchaService.check(data).toRetResult();
    }

    private static String getRemoteId(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        String ip = getRemoteIpFromXForwardedFor(xForwardedFor);
        String ua = request.getHeader("user-agent");
        if (StringUtils.hasText(ip)) {
            return ip + ua;
        }
        return request.getRemoteAddr() + ua;
    }

    private static String getRemoteIpFromXForwardedFor(String xForwardedFor) {
        if (StringUtils.hasText(xForwardedFor)) {
            String[] ipList = xForwardedFor.split(",");
            return ipList[0].strip();
        }
        return null;
    }

}
