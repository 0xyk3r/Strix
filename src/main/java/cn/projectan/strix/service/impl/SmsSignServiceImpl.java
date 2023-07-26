package cn.projectan.strix.service.impl;

import cn.projectan.strix.mapper.SmsSignMapper;
import cn.projectan.strix.model.db.SmsSign;
import cn.projectan.strix.model.system.StrixSmsSign;
import cn.projectan.strix.service.SmsSignService;
import cn.projectan.strix.utils.KeysDiffHandler;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import java.util.List;

/**
 * <p>
 * 服务实现类
 * </p>
 *
 * @author 安炯奕
 * @since 2023-05-20
 */
@Service
public class SmsSignServiceImpl extends ServiceImpl<SmsSignMapper, SmsSign> implements SmsSignService {

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void syncSignList(String configKey, List<StrixSmsSign> signList) {
        List<SmsSign> dbSignList = this.list(new LambdaQueryWrapper<>(SmsSign.class).eq(SmsSign::getConfigKey, configKey));

        List<String> dbSignNameList = dbSignList.stream().map(SmsSign::getName).toList();
        List<String> signNameList = signList.stream().map(StrixSmsSign::getName).toList();

        KeysDiffHandler.handle(dbSignNameList, signNameList,
                (removeKeys) -> {
                    QueryWrapper<SmsSign> removeQueryWrapper = new QueryWrapper<>();
                    removeQueryWrapper.eq("config_key", configKey);
                    removeQueryWrapper.in("name", removeKeys);
                    Assert.isTrue(remove(removeQueryWrapper), "Strix SMS: 同步删除签名失败.");
                },
                (addKeys) -> {
                    List<SmsSign> smsSignList = signList.stream()
                            .filter(s -> addKeys.contains(s.getName()))
                            .map(s -> new SmsSign(s.getCreateTime(), "System", null, "System")
                                    .setConfigKey(configKey)
                                    .setName(s.getName())
                                    .setStatus(s.getStatus())
                            )
                            .toList();
                    Assert.isTrue(saveBatch(smsSignList), "Strix SMS: 同步增加签名失败.");
                }
        );
    }
}
