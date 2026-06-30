package cn.projectan.strix.core.module.ai.livetranslate;

/**
 * 实时语音翻译结果回调。
 * <p>由具体 {@link RealtimeLiveTranslateProvider} 的上游会话在收到结果时调用。
 *
 * @author ProjectAn
 * @since 2026-06-30
 */
public interface LiveTranslateResultListener {

    /**
     * 收到源语言转写结果（流式，需配置 input_audio_transcription.model）。
     *
     * @param result 转写结果（sourceText 有值，translationText 为 null）
     */
    void onSourceTranscript(LiveTranslateResult result);

    /**
     * 收到翻译文本结果（流式或最终）。
     *
     * @param result 翻译结果（translationText 有值，sourceText 可能为 null）
     */
    void onTranslation(LiveTranslateResult result);

    /**
     * 收到翻译音频增量数据（Base64 PCM）。
     *
     * @param responseId 响应 ID
     * @param audioDelta Base64 编码的 PCM 增量数据
     */
    void onAudioDelta(String responseId, String audioDelta);

    /**
     * 翻译音频流结束。
     *
     * @param responseId 响应 ID
     */
    void onAudioDone(String responseId);

    /**
     * 发生错误。
     */
    void onError(String message);

    /**
     * 会话正常结束。
     */
    void onCompleted();
}
