package cn.projectan.strix.model.request.system.module.ai;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

/**
 * 获取模型列表请求
 *
 * @author ProjectAn
 * @since 2026-06-16
 */
@Data
public class AiFetchModelsReq {

    @NotEmpty(message = "Base URL 不能为空")
    private String baseUrl;

    @NotEmpty(message = "API Key 不能为空")
    private String apiKey;

    /**
     * 编辑场景下的配置 ID：当 apiKey 为占位符 {@code __USE_EXISTING__} 时，
     * 后端据此精确取出该配置已存储的 API Key（优先于按 baseUrl 匹配，避免同 baseUrl 多配置取错）。
     */
    @Schema(description = "编辑时的配置 ID（apiKey 为占位符时据此取已存储的 Key）")
    private String configId;
}
