package cn.projectan.strix.core.module.ai.livetranslate;

/**
 * 实时语音翻译单次结果（源语言转写 或 翻译文本）。
 *
 * @param responseId      响应 ID（翻译结果标识，同一段翻译的流式事件共享同一 ID）
 * @param itemId          对话项 ID（源语言转写标识）
 * @param sourceText      源语言识别文本（流式 = text+stash 拼接，最终 = 整句）；仅转写事件有值
 * @param translationText 翻译目标语言文本（流式 = text+stash 拼接，最终 = 整句）；仅翻译事件有值
 * @param stash           待确认的尾部文本（流式中间状态，最终结果为 null）
 * @param isFinal         是否为最终结果（该段识别/翻译已完成）
 * @param sourceLanguage  检测到的源语种代码（zh/en/...）
 * @param removed         是否为撤回信号（模型对误识别的修正，isFinal=true 时 sourceText 为空，前端应移除该句）
 * @author ProjectAn
 * @since 2026-06-30
 */
public record LiveTranslateResult(
        String responseId,
        String itemId,
        String sourceText,
        String translationText,
        String stash,
        boolean isFinal,
        String sourceLanguage,
        boolean removed
) {
    /**
     * 便捷构造器：removed 默认 false
     */
    public LiveTranslateResult(String responseId, String itemId, String sourceText, String translationText,
                               String stash, boolean isFinal, String sourceLanguage) {
        this(responseId, itemId, sourceText, translationText, stash, isFinal, sourceLanguage, false);
    }
}
