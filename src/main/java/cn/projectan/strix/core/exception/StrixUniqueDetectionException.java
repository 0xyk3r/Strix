package cn.projectan.strix.core.exception;

/**
 * @author 安炯奕
 * @date 2021/6/17 15:43
 */
public class StrixUniqueDetectionException extends RuntimeException {

    public StrixUniqueDetectionException() {
        super();
    }

    public StrixUniqueDetectionException(String message) {
        super(message);
    }

    public StrixUniqueDetectionException(String message, Throwable cause) {
        super(message, cause);
    }

    public StrixUniqueDetectionException(Throwable cause) {
        super(cause);
    }

    protected StrixUniqueDetectionException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }

    @Override
    public Throwable fillInStackTrace() {
        return this;
    }
}
