package cn.projectan.strix.core.ret;

/**
 * @author 安炯奕
 * @date 2021/1/31 18:22
 */
public class RetCode {

    public static final int SUCCESS = 200;
    public static final int BAT_REQUEST = 400;
    public static final int NOT_LOGIN = 401;
    public static final int NOT_PERMISSION = 403;
    public static final int NOT_FOUND = 404;
    public static final int METHOD_ERROR = 405;
    public static final int SERVER_ERROR = 500;

    public RetCode() {
    }
}
