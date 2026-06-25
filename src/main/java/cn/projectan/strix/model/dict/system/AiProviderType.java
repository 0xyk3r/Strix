package cn.projectan.strix.model.dict.system;

import cn.projectan.strix.model.annotation.Dict;
import cn.projectan.strix.model.annotation.DictData;
import cn.projectan.strix.model.dict.base.BaseDict;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * AI 云提供商类型
 *
 * @author ProjectAn
 * @since 2026-06-25
 */
@Dict(key = "AiProviderType", value = "AI提供商类型")
@Schema(description = "AI提供商类型")
public class AiProviderType implements BaseDict {

    /**
     * 自动识别（根据 baseUrl 判断，兜底策略）
     */
    @DictData(label = "自动识别", sort = 0, style = DictDataStyle.DEFAULT)
    public static final short AUTO = 0;

    /**
     * 阿里云 DashScope / 百炼
     */
    @DictData(label = "DashScope（阿里云百炼）", sort = 1, style = DictDataStyle.PRIMARY)
    public static final short DASHSCOPE = 1;

    /**
     * DeepSeek 官方 API
     */
    @DictData(label = "DeepSeek", sort = 2, style = DictDataStyle.SUCCESS)
    public static final short DEEPSEEK = 2;

    /**
     * 标准 OpenAI API
     */
    @DictData(label = "OpenAI", sort = 3, style = DictDataStyle.INFO)
    public static final short OPENAI = 3;

    /**
     * 其他 OpenAI 兼容端点
     */
    @DictData(label = "其他兼容端点", sort = 9, style = DictDataStyle.DEFAULT)
    public static final short COMPATIBLE = 9;

}
