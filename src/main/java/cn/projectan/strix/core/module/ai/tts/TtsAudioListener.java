package cn.projectan.strix.core.module.ai.tts;

/**
 * TTS 流式音频结果回调（WebSocket 双向流式 / HTTP 流式共用）。
 *
 * @author ProjectAn
 * @since 2026-06-20
 */
public interface TtsAudioListener {

    /**
     * 收到一段音频字节（PCM/MP3 等，取决于 format）
     */
    void onAudio(byte[] audio);

    /**
     * 合成出错
     */
    void onError(String message);

    /**
     * 合成完成（所有音频已下发）
     */
    void onCompleted();
}
