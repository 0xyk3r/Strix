package cn.projectan.strix.core.ret;

import java.util.List;

/**
 * @author ProjectAn
 * @date 2021/1/31 18:22
 */
public class RetMarker {

    private final static String SUCCESS = "success";

    public static RetResult<Object> makeSuccessRsp() {
        return new RetResult<>(RetCode.SUCCESS, SUCCESS, null);
    }

    public static <T> RetResult<T> makeSuccessRsp(T data) {
        return new RetResult<>(RetCode.SUCCESS, SUCCESS, data);
    }

    public static RetResult<Object> makeErrRsp(int code, String message) {
        return new RetResult<>(code, message, null);
    }

    public static RetResult<Object> makeErrRsp(String message) {
        return new RetResult<>(RetCode.SERVER_ERROR, message, null);
    }

    public static RetResult<Object> makeRsp(int code, String msg) {
        return new RetResult<>(code, msg, null);
    }

    public static <T> RetResult<T> makeRsp(int code, String msg, T data) {
        return new RetResult<>(code, msg, data);
    }

    public static <T> RetPageResult<T> makeSuccessPageRsp(long total, List<T> rows) {
        return new RetPageResult<>(RetCode.SUCCESS, SUCCESS, total, rows);
    }

    public static <T> RetPageResult<T> makeErrPageRsp(String msg) {
        return new RetPageResult<>(RetCode.SERVER_ERROR, msg, 0, null);
    }

}
