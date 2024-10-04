package cn.projectan.strix.core.ret;

/**
 * 响应码
 *
 * @author ProjectAn
 * @since 2021/1/31 18:22
 */
public interface RetCode {

    int SUCCESS = 200;
    int BAT_REQUEST = 400;
    int NOT_LOGIN = 401;
    int NOT_PERMISSION = 403;
    int NOT_FOUND = 404;
    int METHOD_ERROR = 405;
    int SERVER_ERROR = 500;

}
