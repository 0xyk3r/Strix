package cn.projectan.strix.core.module.ai.stt;

import cn.projectan.strix.model.db.system.AiModelConfig;

/**
 * 离线语音识别（STT）平台 Provider 抽象。
 * <p>
 * 不同模型（DashScope Fun-ASR / Paraformer / Qwen-Filetrans 异步、Qwen-Flash 同步、未来其他厂商）
 * 的请求体、结果结构、调用流程不同。本接口将「按配置选择模型 + 转写音频」统一抽象，使
 * {@code DashScopeAiService} 与具体协议解耦。
 * <p>
 * 所有实现注册为 Spring Bean，{@code DashScopeAiService} 按 {@link #supports} 选首个匹配的 Provider。
 *
 * @author ProjectAn
 * @since 2026-06-19
 */
public interface OfflineSttProvider {

    /**
     * 判断该 Provider 是否支持给定模型配置（通常依据 modelName）。实现须排除含 {@code realtime} 的实时模型。
     */
    boolean supports(AiModelConfig config);

    /**
     * 对一个公网可访问的音频 URL 进行转写。
     *
     * @param config   STT 模型配置（含 apiKey / modelName / baseUrl / language 等）
     * @param audioUrl 公网可访问的音频 URL（OSS 签名 URL）
     * @param params   合并后的有效参数（请求覆盖模型默认）
     * @return 结构化识别结果
     */
    SttResult transcribe(AiModelConfig config, String audioUrl, SttParams params);
}
