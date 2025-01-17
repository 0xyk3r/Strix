package cn.projectan.strix.service.impl;

import cn.projectan.strix.mapper.SmsSignMapper;
import cn.projectan.strix.model.constant.OperatorType;
import cn.projectan.strix.model.db.SmsSign;
import cn.projectan.strix.model.other.module.sms.StrixSmsSign;
import cn.projectan.strix.service.SmsSignService;
import cn.projectan.strix.util.algo.KeyDiffUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import java.util.List;
import java.util.stream.Collectors;

/**
 * <p>
 * 服务实现类
 * </p>
 *
 * @author ProjectAn
 * @since 2023-05-20
 */
@Service
public class SmsSignServiceImpl extends ServiceImpl<SmsSignMapper, SmsSign> implements SmsSignService {

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void syncSignList(String configKey, List<StrixSmsSign> signList) {
        List<SmsSign> dbSignList = lambdaQuery()
                .eq(SmsSign::getConfigKey, configKey)
                .list();

        List<String> dbSignNameList = dbSignList.stream().map(SmsSign::getName).collect(Collectors.toList());
        List<String> signNameList = signList.stream().map(StrixSmsSign::getName).collect(Collectors.toList());

        KeyDiffUtil.handle(dbSignNameList, signNameList,
                (removeKeys) -> {
                    Assert.isTrue(
                            this.lambdaUpdate()
                                    .eq(SmsSign::getConfigKey, configKey)
                                    .in(SmsSign::getName, removeKeys)
                                    .remove(),
                            "Strix SMS: 同步删除签名失败.");
                },
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
