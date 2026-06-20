package cn.projectan.strix.model.dict.system;

import cn.projectan.strix.model.annotation.Dict;
import cn.projectan.strix.model.annotation.DictData;
import cn.projectan.strix.model.dict.base.BaseDict;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * AI TTS 音色类型
 *
 * @author ProjectAn
 * @since 2026-06-20
 */
@Dict(key = "AiTtsVoiceType", value = "AI TTS音色类型")
@Schema(description = "AI TTS音色类型")
public class AiTtsVoiceType implements BaseDict {

    /**
     * 声音复刻（基于参考音频复制音色）
     */
    @DictData(label = "声音复刻", sort = 1, style = DictDataStyle.PRIMARY)
    public static final
    short CLONE = 1;

    /**
     * 声音设计（基于文字描述生成音色）
     */
    @DictData(label = "声音设计", sort = 2, style = DictDataStyle.SUCCESS)
    public static final
    short DESIGN = 2;

    public static boolean valid(short value) {
        return value == CLONE || value == DESIGN;
    }

}
