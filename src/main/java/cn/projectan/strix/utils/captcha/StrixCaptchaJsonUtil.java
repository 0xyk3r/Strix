package cn.projectan.strix.utils.captcha;

import cn.projectan.strix.model.other.captcha.CaptchaPointVO;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


/**
 * Strix Captcha Json 工具类
 *
 * @author ProjectAn
 * @since 2024/3/30 13:00
 */
@Slf4j
public class StrixCaptchaJsonUtil {

    public static List<CaptchaPointVO> parseArray(String text, Class<CaptchaPointVO> clazz) {
        if (text == null) {
            return null;
        } else {
            String[] arr = text.replaceFirst("\\[", "")
                    .replaceFirst("]", "").split("}");
            List<CaptchaPointVO> ret = new ArrayList<>(arr.length);
            for (String s : arr) {
                ret.add(parseObject(s, CaptchaPointVO.class));
            }
            return ret;
        }
    }

    public static CaptchaPointVO parseObject(String text, Class<CaptchaPointVO> clazz) {
        if (text == null) {
            return null;
        }
        try {
            CaptchaPointVO ret = clazz.getDeclaredConstructor().newInstance();
            return ret.parse(text);
        } catch (Exception ex) {
            log.error("Strix Captcha: json解析异常", ex);

        }
        return null;
    }

    @SuppressWarnings("unchecked")
    public static String toJSONString(Object object) {
        if (object == null) {
            return "{}";
        }
        if (object instanceof CaptchaPointVO t) {
            return t.toJsonString();
        }
        if (object instanceof List) {
            List<CaptchaPointVO> list = (List<CaptchaPointVO>) object;
            return "[" + list.stream()
                    .map(CaptchaPointVO::toJsonString)
                    .collect(Collectors.joining(",")) + "]";
        }
        if (object instanceof Map) {
            return ((Map<?, ?>) object).entrySet().toString();
        }
        throw new UnsupportedOperationException("不支持的输入类型:"
                + object.getClass().getSimpleName());
    }
}
