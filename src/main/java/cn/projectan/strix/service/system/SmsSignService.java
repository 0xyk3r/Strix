package cn.projectan.strix.service.system;

import cn.projectan.strix.mapper.system.SmsSignMapper;
import cn.projectan.strix.model.constant.system.OperatorType;
import cn.projectan.strix.model.db.system.SmsSign;
import cn.projectan.strix.model.enums.common.NumCategory;
import cn.projectan.strix.model.other.system.module.sms.StrixSmsSign;
import cn.projectan.strix.model.request.system.module.sms.SmsSignListReq;
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
 * Strix SMS 短信签名 服务类
 * </p>
 *
 * @author ProjectAn
 * @since 2023-05-20
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SmsSignService extends ServiceImpl<SmsSignMapper, SmsSign> {

    /**
     * 根据配置key查询签名列表
     *
     * @param configKey 短信配置key
     * @return 签名列表
     */
    public List<SmsSign> listByConfigKey(String configKey) {
        return lambdaQuery()
                .eq(SmsSign::getConfigKey, configKey)
                .list();
    }

    /**
     * 分页查询签名列表
     *
     * @param req 查询请求
     * @return 分页数据
     */
    public Page<SmsSign> listPage(SmsSignListReq req) {
        return lambdaQuery()
                .like(StringUtils.hasText(req.getKeyword()), SmsSign::getName, req.getKeyword())
                .eq(NumUtil.checkCategory(req.getStatus(), NumCategory.POSITIVE), SmsSign::getStatus, req.getStatus())
                .eq(StringUtils.hasText(req.getConfigKey()), SmsSign::getConfigKey, req.getConfigKey())
                .page(req.getPage());
    }

    /**
     * 同步签名列表
     *
     * @param configKey 短信配置key
     * @param signList  签名列表
     */
    @Transactional(rollbackFor = Exception.class)
    public void syncSignList(String configKey, List<StrixSmsSign> signList) {
        List<SmsSign> dbSignList = lambdaQuery()
                .eq(SmsSign::getConfigKey, configKey)
                .list();

        List<String> dbSignNameList = dbSignList.stream().map(SmsSign::getName).collect(Collectors.toList());
        List<String> signNameList = signList.stream().map(StrixSmsSign::getName).collect(Collectors.toList());

        KeyDiffUtil.handle(dbSignNameList, signNameList,
                (removeKeys) ->
                        Assert.isTrue(
                                this.lambdaUpdate()
                                        .eq(SmsSign::getConfigKey, configKey)
                                        .in(SmsSign::getName, removeKeys)
                                        .remove(),
                                "Strix SMS: 同步删除签名失败."),
                (addKeys) -> {
                    List<SmsSign> smsSignList = signList.stream()
                            .filter(s -> addKeys.contains(s.getName()))
                            .map(s -> new SmsSign()
                                    .setConfigKey(configKey)
                                    .setName(s.getName())
                                    .setStatus(s.getStatus())
                                    .setCreatedTime(s.getCreatedTime())
                                    .setCreatedByType(OperatorType.SYSTEM)
                                    .setUpdatedByType(OperatorType.SYSTEM)
                            )
                            .collect(Collectors.toList());
                    Assert.isTrue(saveBatch(smsSignList), "Strix SMS: 同步增加签名失败.");
                }
        );
    }
}
