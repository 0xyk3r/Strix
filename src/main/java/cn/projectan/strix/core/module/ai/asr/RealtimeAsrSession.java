package cn.projectan.strix.core.module.ai.asr;

/**
 * 实时 ASR 上游会话。
 * <p>由 {@link RealtimeAsrProvider#open} 创建，代表与某个 ASR 平台建立的一次实时识别会话。
 * 上层将浏览器上行的 PCM 音频帧通过 {@link #sendAudio} 转发，结果经回调返回。
 *
 * @author ProjectAn
 * @since 2026-06-17
 */
public interface RealtimeAsrSession {

    /**
     * 发送一帧 PCM 16kHz 单声道 16-bit（小端）音频。具体编码/封装由各平台实现负责。
     */
    void sendAudio(byte[] pcm);

    /**
     * 通知音频结束（用于非 VAD 模式提交缓冲；VAD 模式可空实现）。
     */
    void finish();

    /**
     * 关闭会话并释放上游连接。幂等。
     */
    void close();
}
