package cn.projectan.strix.core.ret;

/**
 * 响应生成器
 *
 * @author ProjectAn
 * @since 2021/1/31 18:22
 */
public class RetBuilder {

    private final static String SUCCESS = "success";

    public static RetResult<Object> success() {
        return new RetResult<>(RetCode.SUCCESS, SUCCESS, null);
    }

    public static <T> RetResult<T> success(T data) {
        return new RetResult<>(RetCode.SUCCESS, SUCCESS, data);
    }

    public static RetResult<Object> error(int code, String message) {
        return new RetResult<>(code, message, null);
    }

    public static RetResult<Object> error(String message) {
        return new RetResult<>(RetCode.SERVER_ERROR, message, null);
    }

    public static RetResult<Object> build(int code, String msg) {
        return new RetResult<>(code, msg, null);
    }

    public static <T> RetResult<T> build(int code, String msg, T data) {
        return new RetResult<>(code, msg, data);
    }

}
