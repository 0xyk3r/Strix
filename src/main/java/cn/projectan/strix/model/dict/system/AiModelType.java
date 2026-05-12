package cn.projectan.strix.model.dict.system;

import cn.projectan.strix.model.annotation.Dict;
import cn.projectan.strix.model.annotation.DictData;
import cn.projectan.strix.model.dict.base.BaseDict;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * AI 模型类型
 *
 * @author ProjectAn
 * @since 2026-05-12
 */
@Dict(key = "AiModelType", value = "AI模型类型")
@Schema(description = "AI模型类型")
public class AiModelType implements BaseDict {

    /**
     * 文本模型（对话/文本生成）
     */
    @DictData(label = "文本模型", sort = 1, style = DictDataStyle.PRIMARY)
    public static final
    short TEXT = 1;

    /**
     * 视觉模型（图片/视频理解）
     */
    @DictData(label = "视觉模型", sort = 2, style = DictDataStyle.SUCCESS)
    public static final
    short VISION = 2;

    /**
     * 语音合成 TTS
     */
    @DictData(label = "语音合成 TTS", sort = 3, style = DictDataStyle.WARNING)
    public static final
    short TTS = 3;

    /**
     * 语音识别 STT
     */
    @DictData(label = "语音识别 STT", sort = 4, style = DictDataStyle.WARNING)
    public static final
    short STT = 4;

    /**
     * 图片生成
     */
    @DictData(label = "图片生成", sort = 5, style = DictDataStyle.INFO)
    public static final
    short IMAGE_GEN = 5;

    public static boolean valid(short value) {
        return value == TEXT || value == VISION || value == TTS || value == STT || value == IMAGE_GEN;
    }

}
