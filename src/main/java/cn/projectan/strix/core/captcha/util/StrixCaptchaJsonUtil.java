package cn.projectan.strix.core.captcha.util;

import cn.projectan.strix.model.other.system.captcha.StrixCaptchaPointVO;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


/**
 * Strix Captcha JSON 工具类
 *
 * @author ProjectAn
 * @since 2024/3/30 13:00
 */
@Slf4j
public class StrixCaptchaJsonUtil {

    public static List<StrixCaptchaPointVO> parseArray(String text, Class<StrixCaptchaPointVO> clazz) {
        if (text == null) {
            return null;
        } else {
            String[] arr = text.replaceFirst("\\[", "")
                    .replaceFirst("]", "").split("}");
            List<StrixCaptchaPointVO> ret = new ArrayList<>(arr.length);
            for (String s : arr) {
                ret.add(parseObject(s, StrixCaptchaPointVO.class));
            }
            return ret;
        }
    }

    public static StrixCaptchaPointVO parseObject(String text, Class<StrixCaptchaPointVO> clazz) {
        if (text == null) {
            return null;
        }
        try {
            StrixCaptchaPointVO ret = clazz.getDeclaredConstructor().newInstance();
            return ret.parse(text);
        } catch (Exception ex) {
            log.warn("Strix Captcha: json解析异常: {}", ex.getMessage());
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    public static String toJSONString(Object object) {
        switch (object) {
            case null -> {
                return "{}";
            }
            case StrixCaptchaPointVO obj -> {
                return obj.toJsonString();
            }
            case List<?> obj -> {
                List<StrixCaptchaPointVO> list = (List<StrixCaptchaPointVO>) obj;
                return "[" + list.stream()
                        .map(StrixCaptchaPointVO::toJsonString)
                        .collect(Collectors.joining(",")) + "]";
            }
            case Map<?, ?> obj -> {
                return obj.entrySet().toString();
            }
            default -> {
            }
        }
        throw new UnsupportedOperationException("不支持的输入类型:"
                + object.getClass().getSimpleName());
    }
}
