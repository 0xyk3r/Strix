package cn.projectan.strix.model.request.system.module.ai;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

/**
 * STT 语音识别请求（通过 multipart/form-data 上传音频文件）
 *
 * @author ProjectAn
 * @since 2026-05-12
 */
@Schema(description = "STT 语音识别请求")
@Data
public class AiSttRecognizeReq {

    @Schema(description = "STT 模型配置 Key", example = "paraformer-realtime-default")
    @NotEmpty(message = "模型配置 Key 不能为空")
    private String configKey;

}
