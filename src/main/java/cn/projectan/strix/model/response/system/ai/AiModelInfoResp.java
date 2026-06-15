package cn.projectan.strix.model.response.system.ai;

import lombok.Data;

/**
 * 模型信息响应
 *
 * @author ProjectAn
 * @since 2026-06-16
 */
@Data
public class AiModelInfoResp {

    /**
     * 模型 ID (如 qwen-turbo, gpt-4o)
     */
    private String id;

    /**
     * 显示名称
     */
    private String name;

    /**
     * 所属方 (如 alibaba, openai)
     */
    private String ownedBy;

    /**
     * 创建时间戳
     */
    private Long created;

    /**
     * 模型类型 (1=TEXT 2=VISION 3=TTS 4=STT 5=IMAGE_GEN)
     */
    private Integer type;
}
