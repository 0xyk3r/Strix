package cn.projectan.strix.model.request.system.module.ai;

import cn.projectan.strix.model.annotation.XssIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * TTS 语音合成请求
 *
 * @author ProjectAn
 * @since 2026-05-12
 */
@Schema(description = "TTS 语音合成请求")
@Data
public class AiTtsSynthesizeReq {

    @Schema(description = "TTS 模型配置 Key", example = "qwen-tts-default")
    @NotEmpty(message = "模型配置 Key 不能为空")
    private String configKey;

    @Schema(description = "要合成的文本", example = "欢迎使用 Strix 智能助手")
    @NotEmpty(message = "文本内容不能为空")
    @Size(max = 10000, message = "文本内容过长")
    @XssIgnore
    private String text;

    @Schema(description = "音色 ID（声音复刻/设计的 voice_id，覆盖模型默认音色）")
    private String voiceId;

    @Schema(description = "会话级覆盖参数（JSON 文本，如 {\"rate\":1.2,\"instruction\":\"用激昂的语气\"}）")
    private String params;

}
