package cn.projectan.strix.core.module.ai.provider;

import cn.projectan.strix.model.db.system.AiModelConfig;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * 兜底适配器：标准 OpenAI 及其他兼容端点
 * <p>
 * 仅使用 {@link BaseOpenAICompatAdapter} 定义的标准 OpenAI 参数，无额外特有参数。
 * {@code @Order(Integer.MAX_VALUE)} 确保此 bean 排在所有专用 adapter 之后，
 * 仅在无匹配专用 adapter 时生效。
 *
 * @author ProjectAn
 * @since 2026-06-25
 */
@Order(Integer.MAX_VALUE)
@Component
public class DefaultOpenAIProviderAdapter extends BaseOpenAICompatAdapter {

    @Override
    public boolean supports(AiModelConfig config) {
        // 兜底：总是返回 true
        return true;
    }
}
