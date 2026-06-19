package cn.projectan.strix.core.module.ai.asr;

import java.util.List;

/**
 * 实时 ASR 单次转写结果。
 * <p>由具体 {@link RealtimeAsrProvider} 的上游会话在收到识别结果时构造，
 * 经 {@link AsrResultListener#onTranscript(AsrTranscript)} 回调上层（WebSocket 处理器）转发给浏览器。
 *
 * @param itemId            对话项 ID。同一句的多次中间结果共享同一 itemId，供前端按句聚合；不支持的平台可用自造的稳定 ID
 * @param text              当前文本。中间结果为该句累积文本（替换式，非追加），最终结果为整句（含标点）
 * @param isFinal           是否为该句的最终结果
 * @param emotion           情绪。Qwen-ASR 为 7 类细粒度（surprised/neutral/happy/sad/disgusted/angry/fearful）；
 *                          Paraformer-8k-v2 为 3 类极性（positive/negative/neutral）；不支持的平台为 {@code null}
 * @param emotionScheme     情绪取值方案：{@code "qwen7"} | {@code "polarity3"}；无情绪时 {@code null}，供前端选择映射表
 * @param emotionConfidence 情绪置信度 [0,1]，仅 Paraformer 返回；Qwen / 无情绪为 {@code null}
 * @param language          语种代码（zh/en/ja/...）；未知为 {@code null}
 * @param beginTime         句级开始时间(ms)；不支持时间戳的平台（Qwen）为 {@code null}
 * @param endTime           句级结束时间(ms)；中间结果可能为 {@code null}
 * @param words             字级时间戳数组；不支持的平台（Qwen）为 {@code null}
 * @author ProjectAn
 * @since 2026-06-19
 */
public record AsrTranscript(
        String itemId,
        String text,
        boolean isFinal,
        String emotion,
        String emotionScheme,
        Double emotionConfidence,
        String language,
        Long beginTime,
        Long endTime,
        List<AsrWord> words
) {
}
