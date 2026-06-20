package cn.projectan.strix.model.response.system.ai;

import cn.projectan.strix.model.db.system.AiTtsVoice;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * AI TTS 自定义音色响应
 *
 * @author ProjectAn
 * @since 2026-06-20
 */
@Schema(description = "AI TTS 自定义音色")
@Data
public class AiTtsVoiceResp {

    @Schema(description = "音色记录 ID")
    private String id;

    @Schema(description = "关联 TTS 模型配置 Key")
    private String configKey;

    @Schema(description = "DashScope 音色 ID（voice_id）")
    private String voiceId;

    @Schema(description = "音色显示名称")
    private String name;

    @Schema(description = "音色类型：1=声音复刻 2=声音设计")
    private Short voiceType;

    @Schema(description = "绑定的语音合成模型")
    private String targetModel;

    @Schema(description = "复刻参考音频 URL（声音复刻）")
    private String promptAudioUrl;

    @Schema(description = "声音描述文本（声音设计）")
    private String voicePrompt;

    @Schema(description = "预览文本（声音设计）")
    private String previewText;

    @Schema(description = "音色状态：DEPLOYING/OK/UNDEPLOYED")
    private String status;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "创建时间")
    private LocalDateTime createdTime;

    public static AiTtsVoiceResp from(AiTtsVoice v) {
        AiTtsVoiceResp resp = new AiTtsVoiceResp();
        resp.setId(v.getId());
        resp.setConfigKey(v.getConfigKey());
        resp.setVoiceId(v.getVoiceId());
        resp.setName(v.getName());
        resp.setVoiceType(v.getVoiceType());
        resp.setTargetModel(v.getTargetModel());
        resp.setPromptAudioUrl(v.getPromptAudioUrl());
        resp.setVoicePrompt(v.getVoicePrompt());
        resp.setPreviewText(v.getPreviewText());
        resp.setStatus(v.getStatus());
        resp.setRemark(v.getRemark());
        resp.setCreatedTime(v.getCreatedTime());
        return resp;
    }
}
