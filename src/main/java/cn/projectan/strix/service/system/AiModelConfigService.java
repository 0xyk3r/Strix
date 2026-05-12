package cn.projectan.strix.service.system;

import cn.projectan.strix.core.module.ai.AiModelStore;
import cn.projectan.strix.mapper.system.AiModelConfigMapper;
import cn.projectan.strix.model.db.system.AiModelConfig;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

/**
 * AI 模型配置服务
 *
 * @author ProjectAn
 * @since 2026-05-12
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiModelConfigService extends ServiceImpl<AiModelConfigMapper, AiModelConfig> {

    private final AiModelStore aiModelStore;

    /**
     * 根据 key 查询配置
     */
    public AiModelConfig getByKey(String key) {
        return lambdaQuery()
                .eq(AiModelConfig::getKey, key)
                .one();
    }

    /**
     * 根据 key 获取已启用的配置，不存在或未启用则抛出异常
     */
    public AiModelConfig requireEnabledByKey(String key) {
        AiModelConfig config = getByKey(key);
        Assert.notNull(config, "AI 模型配置不存在: " + key);
        Assert.isTrue(config.getStatus() != null && config.getStatus() == 1, "AI 模型配置未启用: " + key);
        return config;
    }

    /**
     * 保存配置后清除缓存
     */
    public boolean saveAndInvalidate(AiModelConfig config) {
        boolean result = save(config);
        if (result) {
            aiModelStore.invalidate(config.getKey());
        }
        return result;
    }

    /**
     * 更新配置后清除缓存
     */
    public boolean updateAndInvalidate(AiModelConfig config) {
        boolean result = updateById(config);
        if (result) {
            aiModelStore.invalidate(config.getKey());
        }
        return result;
    }

    /**
     * 更新音色 ID（TTS 音色注册完成后调用），同时清除缓存
     *
     * @param id      配置 ID
     * @param voiceId 注册得到的 voice_id
     */
    public boolean updateVoice(String id, String voiceId) {
        AiModelConfig existing = getById(id);
        AiModelConfig update = new AiModelConfig();
        update.setId(id);
        update.setVoice(voiceId);
        boolean result = updateById(update);
        if (result && existing != null) {
            aiModelStore.invalidate(existing.getKey());
        }
        return result;
    }

    /**
     * 删除配置后清除缓存
     */
    public boolean removeAndInvalidate(String id) {
        AiModelConfig config = getById(id);
        boolean result = removeById(id);
        if (result && config != null) {
            aiModelStore.invalidate(config.getKey());
        }
        return result;
    }

}
