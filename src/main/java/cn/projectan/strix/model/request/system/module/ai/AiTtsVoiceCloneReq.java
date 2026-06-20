package cn.projectan.strix.model.request.system.module.ai;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 声音复刻请求（直接使用公网音频 URL；上传文件场景走 multipart 接口）
 *
 * @author ProjectAn
 * @since 2026-06-20
 */
@Schema(description = "声音复刻请求（公网音频 URL）")
@Data
public class AiTtsVoiceCloneReq {

    @Schema(description = "TTS 模型配置 Key")
    @NotEmpty(message = "模型配置 Key 不能为空")
    private String configKey;

    @Schema(description = "音色显示名称")
    @NotEmpty(message = "音色名称不能为空")
    @Size(max = 128, message = "音色名称过长")
    private String name;

    @Schema(description = "参考音频公网 URL（10~20 秒，≤10MB）")
    @NotEmpty(message = "参考音频 URL 不能为空")
    private String audioUrl;

    @Schema(description = "备注")
    @Size(max = 512, message = "备注过长")
    private String remark;

}
