package cn.projectan.strix.core.exception;

/**
 * @author ProjectAn
 * @date 2021/6/17 15:43
 */
public class StrixUniqueDetectionException extends StrixException {

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

}
