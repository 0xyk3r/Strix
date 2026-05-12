package cn.projectan.strix.model.request.system.module.ai;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * 图片生成请求
 *
 * @author ProjectAn
 * @since 2026-05-21
 */
@Schema(description = "图片生成请求")
@Data
public class AiImageGenerateReq {

    @Schema(description = "图片生成模型配置 Key", example = "default-image-gen")
    @NotEmpty(message = "模型配置 Key 不能为空")
    private String configKey;

    @Schema(description = "参考图片 URL 列表（qwen-image-2.0-pro 支持多张）")
    private List<String> imageUrls;

    @Schema(description = "文字提示词", example = "图1中的女生穿着图2中的黑色裙子按图3的姿势坐下")
    @NotEmpty(message = "提示词不能为空")
    @Size(max = 2000, message = "提示词过长")
    private String prompt;

    @Schema(description = "图片尺寸，格式为 宽*高", example = "1024*1024",
            allowableValues = {"1024*1024", "1280*720", "720*1280", "1088*832", "832*1088"})
    private String size;

}
