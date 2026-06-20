package cn.projectan.strix.core.module.ai.tts;

import cn.projectan.strix.model.db.system.AiModelConfig;

/**
 * 语音合成（TTS）平台 Provider 抽象。
 * <p>
 * 不同模型（DashScope CosyVoice / Qwen-TTS / MiniMax、未来其他厂商）的请求体、协议、
 * 流式能力不同。本接口将「按配置选择模型 + 合成语音」统一抽象，使 {@code DashScopeAiService}
 * 与具体协议解耦。三种合成方式分别对应非流式 HTTP、HTTP 流式（SSE）、WebSocket 双向流式。
 * <p>
 * 所有实现注册为 Spring Bean，调用方按 {@link #supports} 选首个匹配的 Provider。
 *
 * @author ProjectAn
 * @since 2026-06-20
 */
public interface TtsProvider {

    /**
     * 判断该 Provider 是否支持给定模型配置（通常依据 modelName）。
     */
    boolean supports(AiModelConfig config);

    /**
     * 非流式合成：发送完整文本，阻塞返回完整音频字节。
     *
     * @param config 模型配置（含 apiKey / modelName / baseUrl）
     * @param text   待合成文本
     * @param params 合并后的有效参数（会话/请求覆盖模型默认）
     * @return 音频字节
     */
    byte[] synthesize(AiModelConfig config, String text, TtsParams params);

    /**
     * HTTP 流式合成：发送完整文本，逐段回调音频字节（SSE）。
     *
     * @param config   模型配置
     * @param text     待合成文本
     * @param params   合并后的有效参数
     * @param listener 流式音频回调
     */
    void synthesizeStream(AiModelConfig config, String text, TtsParams params, TtsAudioListener listener);

    /**
     * WebSocket 双向流式：建立会话，支持流式追加文本与流式接收音频。
     *
     * @param config   模型配置
     * @param params   合并后的有效参数
     * @param listener 流式音频回调
     * @return 流式会话句柄（用于上行文本与结束）
     */
    TtsStreamSession openStream(AiModelConfig config, TtsParams params, TtsAudioListener listener);
}
