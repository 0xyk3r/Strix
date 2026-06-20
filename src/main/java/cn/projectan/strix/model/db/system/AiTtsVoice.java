package cn.projectan.strix.model.db.system;

import cn.projectan.strix.model.db.base.BaseModel;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.io.Serial;

/**
 * AI TTS 自定义音色（声音复刻 / 声音设计）
 *
 * @author ProjectAn
 * @since 2026-06-20
 */
@Getter
@Setter
@Accessors(chain = true)
@NoArgsConstructor
@AllArgsConstructor
@TableName("sys_ai_tts_voice")
public class AiTtsVoice extends BaseModel<AiTtsVoice> {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 关联 TTS 模型配置 ID
     */
    private String configId;

    /**
     * 关联 TTS 模型配置 Key
     */
    private String configKey;

    /**
     * DashScope 音色 ID（voice_id），用于语音合成 voice 参数
     */
    private String voiceId;

    /**
     * 音色显示名称
     */
    @TableField("`name`")
    private String name;

    /**
     * 音色类型
     *
     * @see cn.projectan.strix.model.dict.system.AiTtsVoiceType
     */
    private Short voiceType;

    /**
     * 绑定的语音合成模型（如 cosyvoice-v3.5-plus），合成时须与之一致
     */
    private String targetModel;

    /**
     * 复刻参考音频 URL（声音复刻）
     */
    private String promptAudioUrl;

    /**
     * 声音描述文本（声音设计）
     */
    private String voicePrompt;

    /**
     * 预览文本（声音设计）
     */
    private String previewText;

    /**
     * 音色状态：DEPLOYING（审核中）/ OK（可用）/ UNDEPLOYED（不通过）
     */
    @TableField("`status`")
    private String status;

    /**
     * 备注
     */
    private String remark;

}
