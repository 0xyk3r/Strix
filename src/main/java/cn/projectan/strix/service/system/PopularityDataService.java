package cn.projectan.strix.service.system;

import cn.projectan.strix.mapper.system.PopularityDataMapper;
import cn.projectan.strix.model.db.system.PopularityData;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * <p>
 * Strix 热度工具 数据 服务类
 * </p>
 *
 * @author ProjectAn
 * @since 2023-09-15
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PopularityDataService extends ServiceImpl<PopularityDataMapper, PopularityData> {

    /**
     * 分页查询热度数据
     *
     * @param configKey 配置key
     * @param page      分页参数
     * @return 分页数据
     */
    public Page<PopularityData> listPage(String configKey, Page<PopularityData> page) {
        return lambdaQuery()
                .eq(PopularityData::getConfigKey, configKey)
                .page(page);
    }

    /**
     * 根据配置key删除数据
     *
     * @param configKey 配置key
     */
    public void deleteByConfigKey(String configKey) {
        lambdaUpdate()
                .eq(PopularityData::getConfigKey, configKey)
                .remove();
    }

    /**
     * 更新热度数据原始值
     *
     * @param configKey     配置key
     * @param dataId        数据id
     * @param originalValue 原始值
     */
    public void updateOriginalValue(String configKey, String dataId, Long originalValue) {
        lambdaUpdate()
                .eq(PopularityData::getConfigKey, configKey)
                .eq(PopularityData::getId, dataId)
                .set(PopularityData::getOriginalValue, originalValue)
                .update();
    }

    /**
     * 根据配置key和数据id删除数据
     *
     * @param configKey 配置key
     * @param dataId    数据id
     */
    public void deleteByConfigKeyAndId(String configKey, String dataId) {
        lambdaUpdate()
                .eq(PopularityData::getConfigKey, configKey)
                .eq(PopularityData::getId, dataId)
                .remove();
    }

}
