package cn.projectan.strix.model.dict.system;

import cn.projectan.strix.model.annotation.Dict;
import cn.projectan.strix.model.annotation.DictData;
import cn.projectan.strix.model.dict.base.BaseDict;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * AI 消息状态
 *
 * @author ProjectAn
 * @since 2026-05-12
 */
@Dict(key = "AiMessageStatus", value = "AI消息状态")
@Schema(description = "AI消息状态")
public class AiMessageStatus implements BaseDict {

    @DictData(label = "生成中", sort = 1, style = DictDataStyle.WARNING)
    public static final
    short GENERATING = 0;

    @DictData(label = "已完成", sort = 2, style = DictDataStyle.SUCCESS)
    public static final
    short COMPLETED = 1;

    @DictData(label = "出错", sort = 3, style = DictDataStyle.ERROR)
    public static final
    short ERROR = 2;

}
