package cn.projectan.strix.core.module.ai.asr;

/**
 * 实时 ASR 单次转写结果。
 * <p>由具体 {@link RealtimeAsrProvider} 的上游会话在收到识别结果时构造，
 * 经 {@link AsrResultListener#onTranscript(AsrTranscript)} 回调上层（WebSocket 处理器）转发给浏览器。
 *
 * @param itemId   对话项 ID。同一句的多次中间结果共享同一 itemId，供前端按句聚合；不支持的平台可用自造的稳定 ID
 * @param text     当前文本。中间结果为该句累积文本（替换式，非追加），最终结果为整句（含标点）
 * @param isFinal  是否为该句的最终结果
 * @param emotion  情绪。Qwen-ASR 顶层 {@code emotion}，取 7 类之一（surprised/neutral/happy/sad/disgusted/angry/fearful）；不支持情绪的平台为 {@code null}
 * @param language 语种代码（zh/en/ja/...）；未知为 {@code null}
 * @author ProjectAn
 * @since 2026-06-19
 */
public record AsrTranscript(String itemId, String text, boolean isFinal, String emotion, String language) {
}
