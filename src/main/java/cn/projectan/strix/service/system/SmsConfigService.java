package cn.projectan.strix.service.system;

import cn.projectan.strix.core.module.sms.SmsClientFactory;
import cn.projectan.strix.core.module.sms.StrixSmsClient;
import cn.projectan.strix.core.module.sms.StrixSmsStore;
import cn.projectan.strix.mapper.system.SmsConfigMapper;
import cn.projectan.strix.model.db.system.SmsConfig;
import cn.projectan.strix.model.db.system.SmsSign;
import cn.projectan.strix.model.db.system.SmsTemplate;
import cn.projectan.strix.model.request.system.module.sms.SmsConfigListReq;
import cn.projectan.strix.model.response.common.CommonSelectDataResp;
import cn.projectan.strix.task.system.StrixSmsTask;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * <p>
 * Strix SMS 配置 服务类
 * </p>
 *
 * @author ProjectAn
 * @since 2021-05-02
 */
@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "strix.module", name = "sms", havingValue = "true")
public class SmsConfigService extends ServiceImpl<SmsConfigMapper, SmsConfig> {

    private final SmsSignService smsSignService;
    private final SmsTemplateService smsTemplateService;
    @Lazy
    private final StrixSmsTask strixSmsTask;
    private final StrixSmsStore strixSmsStore;
    private final List<SmsClientFactory> smsClientFactories;

    private Map<Short, SmsClientFactory> factoryMap;

    private Map<Short, SmsClientFactory> getFactoryMap() {
        if (factoryMap == null) {
            factoryMap = smsClientFactories.stream()
                    .collect(Collectors.toMap(SmsClientFactory::supportedPlatform, Function.identity()));
        }
        return factoryMap;
    }

    /**
     * 创建实例
     *
     * @param smsConfigList 短信配置列表
     */
    public void createInstance(List<SmsConfig> smsConfigList) {
        for (SmsConfig smsConfig : smsConfigList) {
            try {
                SmsClientFactory factory = getFactoryMap().get(smsConfig.getPlatform());
                if (factory == null) {
                    log.error("Strix SMS: 初始化短信服务实例 <{}> 失败. (不支持的平台: {})", smsConfig.getKey(), smsConfig.getPlatform());
                    continue;
                }
                StrixSmsClient client = factory.createClient(smsConfig);
                strixSmsStore.addInstance(smsConfig.getKey(), client);
                log.info("Strix SMS: 初始化短信服务实例 <{}> 成功.", smsConfig.getKey());
            } catch (Exception e) {
                log.error("Strix SMS: 初始化短信服务实例 <{}> 失败.", smsConfig.getKey(), e);
            }
        }

        // 全部初始化完成后，进行初始化签名和模板信息
        strixSmsTask.refreshSignList();
        strixSmsTask.refreshTemplateList();
    }

    /**
     * 获取下拉数据
     *
     * @return 下拉数据
     */
    public CommonSelectDataResp getSelectData() {
        List<SmsConfig> smsConfigList = getBaseMapper().selectList(Wrappers.emptyWrapper());
        return new CommonSelectDataResp(smsConfigList, "key", "key", "name");
    }

    /**
     * 分页查询短信配置列表
     *
     * @param req 查询请求
     * @return 分页数据
     */
    public Page<SmsConfig> listPage(SmsConfigListReq req) {
        return lambdaQuery()
                .like(StringUtils.hasText(req.getKeyword()), SmsConfig::getKey, req.getKeyword())
                .or(StringUtils.hasText(req.getKeyword()), q -> q.like(SmsConfig::getName, req.getKeyword()))
                .page(req.getPage());
    }

    /**
     * 删除短信配置及其关联的签名和模板
     *
     * @param smsConfig 待删除的短信配置
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteConfigWithRelations(SmsConfig smsConfig) {
        removeById(smsConfig.getId());
        String key = smsConfig.getKey();
        smsSignService.lambdaUpdate()
                .eq(SmsSign::getConfigKey, key)
                .remove();
        smsTemplateService.lambdaUpdate()
                .eq(SmsTemplate::getConfigKey, key)
                .remove();
    }

}
