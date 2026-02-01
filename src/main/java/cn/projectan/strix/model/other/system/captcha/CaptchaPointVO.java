package cn.projectan.strix.model.other.system.captcha;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;

/**
 * 验证码坐标VO
 *
 * @author ProjectAn
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CaptchaPointVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private String secretKey;

    private int x;

    private int y;

    public CaptchaPointVO(int x, int y) {
        this.x = x;
        this.y = y;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        CaptchaPointVO that = (CaptchaPointVO) o;
        return x == that.x && y == that.y && Objects.equals(secretKey, that.secretKey);
    }

    @Override
    public int hashCode() {
        return Objects.hash(secretKey, x, y);
    }

}
