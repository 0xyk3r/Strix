package cn.projectan.strix.model.other.system.captcha;

import lombok.Data;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Strix 验证码坐标VO
 */
@Data
public class StrixCaptchaPointVO {

    private String secretKey;

    public int x;

    public int y;

    public StrixCaptchaPointVO() {
    }

    public StrixCaptchaPointVO(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public StrixCaptchaPointVO(int x, int y, String secretKey) {
        this.secretKey = secretKey;
        this.x = x;
        this.y = y;
    }

    public String toJsonString() {
        return String.format("{\"secretKey\":\"%s\",\"x\":%d,\"y\":%d}", secretKey, x, y);
    }

    public StrixCaptchaPointVO parse(String jsonStr) {
        Map<String, Object> m = new HashMap<>();
        Arrays.stream(jsonStr
                .replaceFirst(",\\{", "{")
                .replaceFirst("\\{", "")
                .replaceFirst("}", "")
                .replaceAll("\"", "")
                .split(",")).forEach(item -> m.put(item.split(":")[0], item.split(":")[1]));
        // PointVO d = new PointVO();
        setX(Double.valueOf("" + m.get("x")).intValue());
        setY(Double.valueOf("" + m.get("y")).intValue());
        setSecretKey(m.getOrDefault("secretKey", "") + "");
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        StrixCaptchaPointVO strixCaptchaPointVO = (StrixCaptchaPointVO) o;
        return x == strixCaptchaPointVO.x && y == strixCaptchaPointVO.y && Objects.equals(secretKey, strixCaptchaPointVO.secretKey);
    }

    @Override
    public int hashCode() {

        return Objects.hash(secretKey, x, y);
    }
}
