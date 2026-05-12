package cn.projectan.strix.model.response.system.ai;

/**
 * AI SSE 事件类型常量
 *
 * @author ProjectAn
 * @since 2026-05-12
 */
public final class AiSseEvent {

    private AiSseEvent() {
    }

    /**
     * 思考内容块（qwen3-max thinking 模式）
     */
    public static final String THINKING = "thinking";

    /**
     * 正文内容块
     */
    public static final String CONTENT = "content";

    /**
     * 流式完成
     */
    public static final String DONE = "done";

    /**
     * 错误
     */
    public static final String ERROR = "error";
}
