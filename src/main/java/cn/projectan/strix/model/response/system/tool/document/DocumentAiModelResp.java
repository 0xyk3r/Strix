package cn.projectan.strix.model.response.system.tool.document;

import cn.projectan.strix.model.db.system.AiModelConfig;
import lombok.Data;

/**
 * 文档 AI 分析可用模型响应
 *
 * @author ProjectAn
 * @since 2026/6/29
 */
@Data
public class DocumentAiModelResp {

    /**
     * 模型配置 Key
     */
    private String key;

    /**
     * 显示名称
     */
    private String name;

    /**
     * 模型标识（如 qwen-vl-max）
     */
    private String modelName;

    /**
     * 模型类型（1=TEXT 2=VISION）
     */
    private Short type;

    public DocumentAiModelResp(AiModelConfig config) {
        this.key = config.getKey();
        this.name = config.getName();
        this.modelName = config.getModelName();
        this.type = config.getType();
    }

}
