package cn.projectan.strix.core.exception;

import lombok.Getter;

/**
 * 定时任务异常
 */
@Getter
public class StrixJobException extends StrixException {

    private final Code code;

    public StrixJobException(String msg, Code code) {
        this(msg, code, null);
    }

    public StrixJobException(String msg, Code code, Exception nestedEx) {
        super(msg, nestedEx);
        this.code = code;
    }

    public enum Code {
        TASK_EXISTS, NO_TASK_EXISTS, TASK_ALREADY_STARTED, UNKNOWN, CONFIG_ERROR, TASK_NODE_NOT_AVAILABLE
    }

}
