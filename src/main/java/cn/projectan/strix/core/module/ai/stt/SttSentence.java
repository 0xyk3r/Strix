package cn.projectan.strix.core.module.ai.stt;

import java.util.List;

/**
 * 离线 STT 句级结果。
 *
 * @param text      句文本
 * @param beginTime 句级开始时间(ms)；Qwen-Flash 等无时间戳时为 {@code null}
 * @param endTime   句级结束时间(ms)
 * @param speakerId 说话人索引；未启用/不支持说话人分离时为 {@code null}
 * @param emotion   情绪（Qwen 7 类：neutral/happy/...）；Fun-ASR/Paraformer 为 {@code null}
 * @param language  句级语种代码；未知为 {@code null}
 * @param words     字级时间戳；不支持/缺失为 {@code null}
 * @author ProjectAn
 * @since 2026-06-19
 */
public record SttSentence(
        String text,
        Long beginTime,
        Long endTime,
        Integer speakerId,
        String emotion,
        String language,
        List<SttWord> words
) {
}
