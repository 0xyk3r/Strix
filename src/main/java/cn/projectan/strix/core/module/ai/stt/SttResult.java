package cn.projectan.strix.core.module.ai.stt;

import java.util.List;

/**
 * 离线 STT 结构化识别结果。
 * <p>由 {@link OfflineSttProvider} 解析上游响应构造，经 {@link SttResultJson#toJson} 序列化为
 * JSON 字符串存入异步任务结果，前端按结构化形态富展示。
 *
 * @param text       全文（所有句子拼接，便于一键复制）
 * @param durationMs 音频时长(ms)；缺失为 {@code null}
 * @param language   顶层主语种；缺失为 {@code null}
 * @param sentences  逐句结果
 * @author ProjectAn
 * @since 2026-06-19
 */
public record SttResult(
        String text,
        Long durationMs,
        String language,
        List<SttSentence> sentences
) {
}
