package cn.projectan.strix.core.exception;

/**
 * StrixNoAuthException
 *
 * @author ProjectAn
 * @since 2023/2/25 1:57
 */
public class StrixNoAuthException extends StrixException {

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

}
