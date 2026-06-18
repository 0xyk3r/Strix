package cn.projectan.strix.core.module.ai.asr;

/**
 * 实时 ASR 转写结果回调。
 * <p>由具体 {@link RealtimeAsrProvider} 的上游会话在收到识别结果时调用，
 * 上层（WebSocket 处理器）据此转发给浏览器客户端。
 *
 * @author ProjectAn
 * @since 2026-06-17
 */
public interface AsrResultListener {

    /**
     * 收到一段转写结果。
     *
     * @param result 转写结果（含文本、是否最终、情绪、语种等，详见 {@link AsrTranscript}）
     */
    void onTranscript(AsrTranscript result);

    /**
     * 发生错误（上游连接失败、鉴权失败、协议错误等）。
     */
    void onError(String message);

    /**
     * 识别任务正常结束（上游关闭 / 任务完成）。
     */
    void onCompleted();
}
