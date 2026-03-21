package cn.projectan.strix.service.system;

import cn.projectan.strix.mapper.system.SmsTemplateMapper;
import cn.projectan.strix.model.constant.system.OperatorType;
import cn.projectan.strix.model.db.system.SmsTemplate;
import cn.projectan.strix.model.enums.common.NumCategory;
import cn.projectan.strix.model.other.system.module.sms.StrixSmsTemplate;
import cn.projectan.strix.model.request.system.module.sms.SmsTemplateListReq;
import cn.projectan.strix.util.algo.KeyDiffUtil;
import cn.projectan.strix.util.math.NumUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.stream.Collectors;

/**
 * <p>
 * 服务类
 * </p>
 *
 * @author ProjectAn
 * @since 2023-05-20
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SmsTemplateService extends ServiceImpl<SmsTemplateMapper, SmsTemplate> {

    /**
     * 根据配置key查询模板列表
     *
     * @param configKey 短信配置key
     * @return 模板列表
     */
    public List<SmsTemplate> listByConfigKey(String configKey) {
        return lambdaQuery()
                .eq(SmsTemplate::getConfigKey, configKey)
                .list();
    }

    /**
     * 分页查询模板列表
     *
     * @param req 查询请求
     * @return 分页数据
     */
    public Page<SmsTemplate> listPage(SmsTemplateListReq req) {
        return lambdaQuery()
                .like(StringUtils.hasText(req.getKeyword()), SmsTemplate::getName, req.getKeyword())
                .eq(NumUtil.checkCategory(req.getType(), NumCategory.POSITIVE), SmsTemplate::getType, req.getType())
                .eq(NumUtil.checkCategory(req.getStatus(), NumCategory.POSITIVE), SmsTemplate::getStatus, req.getStatus())
                .eq(StringUtils.hasText(req.getConfigKey()), SmsTemplate::getConfigKey, req.getConfigKey())
                .page(req.getPage());
    }

    /**
     * 同步模板列表
     *
     * @param configKey    短信配置key
     * @param templateList 模板列表
     */
    @Transactional(rollbackFor = Exception.class)
    public void syncTemplateList(String configKey, List<StrixSmsTemplate> templateList) {
        List<SmsTemplate> dbTemplateList = lambdaQuery()
                .eq(SmsTemplate::getConfigKey, configKey)
                .list();

        List<String> dbTemplateCodeList = dbTemplateList.stream().map(SmsTemplate::getCode).collect(Collectors.toList());
        List<String> templateCodeList = templateList.stream().map(StrixSmsTemplate::getCode).collect(Collectors.toList());

        KeyDiffUtil.handle(dbTemplateCodeList, templateCodeList,
                (removeKeys) ->
                        Assert.isTrue(
                                this.lambdaUpdate()
                                        .eq(SmsTemplate::getConfigKey, configKey)
                                        .in(SmsTemplate::getCode, removeKeys)
                                        .remove(),
                                "Strix SMS: 同步删除模板失败."),
                (addKeys) -> {
                    List<SmsTemplate> smsTemplateList = templateList.stream()
                            .filter(t -> addKeys.contains(t.getCode()))
                            .map(t -> new SmsTemplate()
                                    .setConfigKey(configKey)
                                    .setCode(t.getCode())
                                    .setName(t.getName())
                                    .setType(t.getType())
                                    .setStatus(t.getStatus())
                                    .setContent(t.getContent())
                                    .setCreatedTime(t.getCreatedTime())
                                    .setCreatedByType(OperatorType.SYSTEM)
                                    .setUpdatedByType(OperatorType.SYSTEM)
                            )
                            .collect(Collectors.toList());
                    Assert.isTrue(saveBatch(smsTemplateList), "Strix SMS: 同步增加模板失败.");
                }
        );
    }

}
