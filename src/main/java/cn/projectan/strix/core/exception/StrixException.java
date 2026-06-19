package cn.projectan.strix.core.exception;

/**
 * StrixException
 *
 * @author ProjectAn
 * @since 2021/5/7 18:21
 */
public class StrixException extends RuntimeException {

    /**
     * 是否保留堆栈跟踪
     */
    private final boolean stackTraceEnabled;

    public StrixException() {
        this.stackTraceEnabled = false;
    }

    public StrixException(String message) {
        super(message);
        this.stackTraceEnabled = false;
    }

    public StrixException(String message, Throwable cause) {
        super(message, cause);
        this.stackTraceEnabled = false;
    }

    public StrixException(Throwable cause) {
        super(cause);
        this.stackTraceEnabled = false;
    }

    /**
     * 创建带堆栈跟踪的业务异常（用于系统级错误，便于排查）
     */
    public static StrixException withStackTrace(String message) {
        return new StrixException(message, null, true);
    }

    /**
     * 创建带堆栈跟踪的业务异常（保留原始异常链）
     */
    public static StrixException withStackTrace(String message, Throwable cause) {
        return new StrixException(message, cause, true);
    }

    protected StrixException(String message, Throwable cause, boolean enableStackTrace) {
        super(message, cause);
        this.stackTraceEnabled = enableStackTrace;
    }

    @Override
    public Throwable fillInStackTrace() {
        return stackTraceEnabled ? super.fillInStackTrace() : this;
    }
}
