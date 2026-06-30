package cn.projectan.strix.core.module.ai.livetranslate;

/**
 * 实时语音翻译上游会话。
 * <p>由 {@link RealtimeLiveTranslateProvider#open} 创建，代表与某个翻译平台建立的一次实时会话。
 * 上层将浏览器上行的 PCM 音频帧通过 {@link #sendAudio} 转发。
 *
 * @author ProjectAn
 * @since 2026-06-30
 */
public interface RealtimeLiveTranslateSession {

    /**
     * 发送一帧 PCM 16kHz 单声道 16-bit（小端）音频。
     */
    void sendAudio(byte[] pcm);

    /**
     * 通知音频结束，触发服务端冲刷最后一段翻译结果。
     */
    void finish();

    /**
     * 关闭会话并释放上游连接。幂等。
     */
    void close();
}
