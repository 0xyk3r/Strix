package cn.projectan.strix.core.module.ai.tts;

/**
 * TTS 双向流式合成会话句柄（上行：流式追加文本）。
 * <p>与 {@link TtsAudioListener} 配合：本接口负责上行文本，listener 负责下行音频。
 *
 * @author ProjectAn
 * @since 2026-06-20
 */
public interface TtsStreamSession {

    /**
     * 流式追加一段待合成文本（可多次调用）
     */
    void sendText(String text);

    /**
     * 通知文本发送完毕，请求结束合成（服务端冲刷剩余缓存）
     */
    void finish();

    /**
     * 关闭会话与底层连接
     */
    void close();
}
