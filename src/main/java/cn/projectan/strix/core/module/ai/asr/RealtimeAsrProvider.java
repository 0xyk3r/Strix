package cn.projectan.strix.core.module.ai.asr;

import cn.projectan.strix.model.db.system.AiModelConfig;

/**
 * 实时语音识别（ASR）平台 Provider 抽象。
 * <p>
 * 不同平台（阿里云百炼 qwen-asr-realtime / paraformer-realtime / fun-asr-realtime、未来其他厂商）使用各自不同的
 * 实时协议。本接口将"按配置选择平台 + 建立实时会话"统一抽象，使
 * {@code AiAsrWebSocketHandler} 与具体平台协议解耦。
 * <p>
 * 所有实现注册为 Spring Bean，处理器按 {@link #supports} 选择首个匹配的 Provider。
 *
 * @author ProjectAn
 * @since 2026-06-17
 */
public interface RealtimeAsrProvider {

    /**
     * 判断该 Provider 是否支持给定模型配置（通常依据 modelName）。
     */
    boolean supports(AiModelConfig config);

    /**
     * 建立一次实时识别会话并连接上游。
     *
     * @param config   ASR 模型配置（含 apiKey / modelName / language 等）
     * @param params   合并后的有效 run-task 参数（会话覆盖模型默认）；不支持参数的平台可忽略
     * @param listener 结果回调
     * @return 会话句柄，用于发送音频与关闭
     */
    RealtimeAsrSession open(AiModelConfig config, AsrSessionParams params, AsrResultListener listener);
}
