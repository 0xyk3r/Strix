package cn.projectan.strix.service.system;

import cn.projectan.strix.core.module.oss.OssClientFactory;
import cn.projectan.strix.core.module.oss.StrixOssClient;
import cn.projectan.strix.core.module.oss.StrixOssStore;
import cn.projectan.strix.mapper.system.OssConfigMapper;
import cn.projectan.strix.model.db.system.OssConfig;
import cn.projectan.strix.model.request.system.module.oss.OssConfigListReq;
import cn.projectan.strix.model.response.common.CommonSelectDataResp;
import cn.projectan.strix.task.system.StrixOssTask;
import cn.projectan.strix.util.algo.KeyDiffUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * <p>
 * Strix OSS 配置 服务类
 * </p>
 *
 * @author ProjectAn
 * @since 2021-05-02
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OssConfigService extends ServiceImpl<OssConfigMapper, OssConfig> {

    private final StrixOssStore strixOssStore;
    @Lazy
    private final StrixOssTask strixOssTask;
    private final List<OssClientFactory> ossClientFactories;

    private Map<Short, OssClientFactory> factoryMap;

    private Map<Short, OssClientFactory> getFactoryMap() {
        if (factoryMap == null) {
            factoryMap = ossClientFactories.stream()
                    .collect(Collectors.toMap(OssClientFactory::supportedPlatform, Function.identity()));
        }
        return factoryMap;
    }

    /**
     * 分页查询存储配置列表
     *
     * @param req 查询请求
     * @return 分页数据
     */
    public Page<OssConfig> listPage(OssConfigListReq req) {
        return lambdaQuery()
                .like(StringUtils.hasText(req.getKeyword()), OssConfig::getKey, req.getKeyword())
                .or(StringUtils.hasText(req.getKeyword()), q -> q.like(OssConfig::getName, req.getKeyword()))
                .page(req.getPage());
    }

    /**
     * 刷新配置
     */
    public void refreshConfig() {
        List<OssConfig> ossConfigList = list();
        List<String> ossConfigKeyList = ossConfigList.stream()
                .map(OssConfig::getKey)
                .collect(Collectors.toList());
        Set<String> instanceKeySet = strixOssStore.getInstanceKeySet();

        KeyDiffUtil.handle(instanceKeySet, ossConfigKeyList,
                (removeKeys) -> removeKeys.forEach(key -> {
                    Optional.ofNullable(strixOssStore.getInstance(key)).ifPresent(StrixOssClient::close);
                    strixOssStore.removeInstance(key);
                }),
                (addKeys) -> {
                    List<OssConfig> addSmsConfigList = ossConfigList.stream().filter(ossConfig -> addKeys.contains(ossConfig.getKey())).collect(Collectors.toList());
                    createInstance(addSmsConfigList);
                });
    }

    /**
     * 创建实例
     *
     * @param ossConfigList 阿里云OSS配置列表
     */
    public void createInstance(List<OssConfig> ossConfigList) {
        for (OssConfig ossConfig : ossConfigList) {
            try {
                OssClientFactory factory = getFactoryMap().get(ossConfig.getPlatform());
                if (factory == null) {
                    log.error("Strix OSS: 初始化对象存储服务实例 <{}> 失败. (不支持的平台: {})", ossConfig.getKey(), ossConfig.getPlatform());
                    continue;
                }
                StrixOssClient client = factory.createClient(ossConfig);
                strixOssStore.addInstance(ossConfig.getKey(), client);
                log.info("Strix OSS: 初始化对象存储服务实例 <{}> 完成.", ossConfig.getKey());
            } catch (Exception e) {
                log.error("Strix OSS: 初始化对象存储服务实例 <{}> 失败.", ossConfig.getKey(), e);
            }
        }

        // 全部初始化完成后，进行下一步操作
        strixOssTask.refreshBucketList();
    }

    /**
     * 获取下拉数据
     *
     * @return 下拉数据
     */
    public CommonSelectDataResp getSelectData() {
        List<OssConfig> ossConfigList = getBaseMapper().selectList(Wrappers.emptyWrapper());
        return new CommonSelectDataResp(ossConfigList, "key", "key", "name");
    }

}
