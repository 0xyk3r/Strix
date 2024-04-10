package cn.projectan.strix.core.ret;


import lombok.Data;

/**
 * 响应信息
 *
 * @author ProjectAn
 * @date 2021/1/31 18:22
 */
@Data
public class RetResult<T> implements java.io.Serializable {

    private int code;
    private String msg;
    private T data;

    public RetResult() {
    }

    public RetResult(int code, String msg, T data) {
        this.code = code;
        this.msg = msg;
        this.data = data;
    }
}
