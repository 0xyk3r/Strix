package cn.projectan.strix.core.module.ai.asr;

/**
 * 字级时间戳。
 *
 * @param beginTime   字开始时间(ms)
 * @param endTime     字结束时间(ms)
 * @param text        该字/词文本
 * @param punctuation 该字后跟随的标点（无则空串）
 * @author ProjectAn
 * @since 2026-06-19
 */
public record AsrWord(Long beginTime, Long endTime, String text, String punctuation) {
}
