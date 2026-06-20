package cn.projectan.strix.model.request.system.module.ai;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 声音设计请求（用文字描述创建音色）
 *
 * @author ProjectAn
 * @since 2026-06-20
 */
@Schema(description = "声音设计请求")
@Data
public class AiTtsVoiceDesignReq {

    @Schema(description = "TTS 模型配置 Key")
    @NotEmpty(message = "模型配置 Key 不能为空")
    private String configKey;

    @Schema(description = "音色显示名称")
    @NotEmpty(message = "音色名称不能为空")
    @Size(max = 128, message = "音色名称过长")
    private String name;

    @Schema(description = "声音描述文本（≤500 字符，仅中英文）", example = "沉稳的中年男性，音色低沉浑厚，富有磁性")
    @NotEmpty(message = "声音描述不能为空")
    @Size(max = 500, message = "声音描述过长")
    private String voicePrompt;

    @Schema(description = "预览音频朗读文本", example = "各位听众朋友，大家好")
    @NotEmpty(message = "预览文本不能为空")
    @Size(max = 200, message = "预览文本过长")
    private String previewText;

    @Schema(description = "备注")
    @Size(max = 512, message = "备注过长")
    private String remark;

}
