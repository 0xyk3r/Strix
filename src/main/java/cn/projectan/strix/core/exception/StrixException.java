package cn.projectan.strix.core.exception;

/**
 * @author ProjectAn
 * @date 2021/5/7 18:21
 */
public class StrixException extends RuntimeException {

    public StrixException() {
        super();
    }

    public StrixException(String message) {
        super(message);
    }

    public StrixException(String message, Throwable cause) {
        super(message, cause);
    }

    public StrixException(Throwable cause) {
        super(cause);
    }

    protected StrixException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }

    @Override
    public Throwable fillInStackTrace() {
        return this;
    }

}
