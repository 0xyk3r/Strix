package cn.projectan.strix.model.response.system.ai;

import cn.projectan.strix.model.db.system.AiSession;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * AI 对话会话响应
 *
 * @author ProjectAn
 * @since 2026-05-12
 */
@Schema(description = "AI 对话会话响应")
@Data
public class AiSessionResp {

    @Schema(description = "会话 ID")
    private String id;

    @Schema(description = "模型配置 ID")
    private String modelConfigId;

    @Schema(description = "模型配置名称")
    private String modelConfigName;

    @Schema(description = "会话标题")
    private String title;

    @Schema(description = "状态：0=活跃 1=已归档")
    private Short status;

    @Schema(description = "创建时间")
    private LocalDateTime createdTime;

    @Schema(description = "更新时间")
    private LocalDateTime updatedTime;

    public static AiSessionResp from(AiSession session) {
        if (session == null) return null;
        AiSessionResp resp = new AiSessionResp();
        resp.setId(session.getId());
        resp.setModelConfigId(session.getModelConfigId());
        resp.setTitle(session.getTitle());
        resp.setStatus(session.getStatus());
        resp.setCreatedTime(session.getCreatedTime());
        resp.setUpdatedTime(session.getUpdatedTime());
        return resp;
    }
}
