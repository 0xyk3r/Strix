package cn.projectan.strix.core.exception;

/**
 * @author 安炯奕
 * @date 2023/2/25 1:57
 */
public class StrixNoAuthException extends RuntimeException {

    public StrixNoAuthException() {
        super();
    }

    public StrixNoAuthException(String message) {
        super(message);
    }

    public StrixNoAuthException(String message, Throwable cause) {
        super(message, cause);
    }

    public StrixNoAuthException(Throwable cause) {
        super(cause);
    }

    protected StrixNoAuthException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }

    @Override
    public Throwable fillInStackTrace() {
        return this;
    }

}
