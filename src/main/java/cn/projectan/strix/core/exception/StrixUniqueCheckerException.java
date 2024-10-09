package cn.projectan.strix.core.exception;

/**
 * Strix 字段唯一性检查器异常
 *
 * @author ProjectAn
 * @since 2021/6/17 15:43
 */
public class StrixUniqueCheckerException extends StrixException {

    public StrixUniqueCheckerException() {
        super();
    }

    public StrixUniqueCheckerException(String message) {
        super(message);
    }

    public StrixUniqueCheckerException(String message, Throwable cause) {
        super(message, cause);
    }

    public StrixUniqueCheckerException(Throwable cause) {
        super(cause);
    }

    protected StrixUniqueCheckerException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }

}
