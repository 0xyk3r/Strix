package cn.projectan.strix.model.response.system.ai;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

/**
 * AI 异步任务状态（TTS 音色注册 / STT 批量转写等长耗时任务）
 * <p>同时作为 Redis 存储对象与接口响应；任务归属通过 Redis key 隔离，不在此暴露 ownerId。
 *
 * @author ProjectAn
 * @since 2026-06-17
 */
@Schema(description = "AI 异步任务状态")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AiTaskStatusResp implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "任务 ID")
    private String taskId;

    @Schema(description = "任务类型：tts-enroll / stt-transcribe")
    private String type;

    @Schema(description = "状态：PENDING（排队）/ RUNNING（执行中）/ SUCCEEDED（成功）/ FAILED（失败）")
    private String status;

    @Schema(description = "成功时的结果（音色 ID 或识别文本）")
    private String result;

    @Schema(description = "失败时的错误信息")
    private String error;
}
