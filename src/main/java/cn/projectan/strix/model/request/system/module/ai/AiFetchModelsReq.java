package cn.projectan.strix.model.request.system.module.ai;

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
}
