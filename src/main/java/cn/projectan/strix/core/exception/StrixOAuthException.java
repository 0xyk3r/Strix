package cn.projectan.strix.core.exception;

/**
 * Strix OAuth 异常
 *
 * @author ProjectAn
 * @since 2026/1/29
 */
public class StrixOAuthException extends StrixException {

    public StrixOAuthException(String message) {
        super(message);
    }

    public StrixOAuthException(String message, Throwable cause) {
        super(message, cause);
    }

}
