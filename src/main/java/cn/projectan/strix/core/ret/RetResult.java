package cn.projectan.strix.core.ret;


import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;

/**
 * 响应信息
 *
 * @author ProjectAn
 * @since 2021/1/31 18:22
 */
@Data
@Schema(description = "统一响应结构")
public class RetResult<T> implements java.io.Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "响应码 (200=成功, 400=参数错误, 401=未登录, 403=无权限, 404=未找到, 429=请求过频, 500=服务器错误)", example = "200")
    private int code;

    @Schema(description = "响应消息", example = "success")
    private String msg;

    @Schema(description = "响应数据")
    private T data;

    public RetResult() {
    }

    public RetResult(int code, String msg, T data) {
        this.code = code;
        this.msg = msg;
        this.data = data;
    }
}
