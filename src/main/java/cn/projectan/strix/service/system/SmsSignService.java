package cn.projectan.strix.service.system;

import cn.projectan.strix.mapper.system.SmsSignMapper;
import cn.projectan.strix.model.constant.system.OperatorType;
import cn.projectan.strix.model.db.system.SmsSign;
import cn.projectan.strix.model.other.system.module.sms.StrixSmsSign;
import cn.projectan.strix.util.algo.KeyDiffUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

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
