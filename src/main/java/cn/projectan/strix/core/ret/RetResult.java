package cn.projectan.strix.core.ret;


import lombok.Data;

/**
 * @author 安炯奕
 * @date 2021/1/31 18:22
 */
@Data
public class RetResult<T> implements java.io.Serializable {

    private int code;
    private String msg;
    private T data;

    /**
     * 不知道为什么 删除默认构造方法 其他模块无法使用其作为返回值 所以加上
     */
    public RetResult() {

    }

    public RetResult(int code, String msg, T data) {
        this.code = code;
        this.msg = msg;
        this.data = data;
    }
}
