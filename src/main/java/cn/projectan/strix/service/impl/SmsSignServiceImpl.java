package cn.projectan.strix.service.impl;

import cn.projectan.strix.mapper.SmsSignMapper;
import cn.projectan.strix.model.db.SmsSign;
import cn.projectan.strix.model.system.StrixSmsSign;
import cn.projectan.strix.service.SmsSignService;
import cn.projectan.strix.utils.KeysDiffHandler;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import java.util.ArrayList;
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
        QueryWrapper<SmsSign> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("config_key", configKey);
        List<SmsSign> dbSignList = this.list(queryWrapper);

        List<String> dbSignNameList = dbSignList.stream().map(SmsSign::getName).toList();
        List<String> signNameList = signList.stream().map(StrixSmsSign::getName).toList();

        KeysDiffHandler.handle(dbSignNameList, signNameList, ((removeKeys, addKeys) -> {
            if (removeKeys.size() > 0) {
                QueryWrapper<SmsSign> removeQueryWrapper = new QueryWrapper<>();
                removeQueryWrapper.eq("config_key", configKey);
                removeQueryWrapper.in("name", removeKeys);
                Assert.isTrue(remove(removeQueryWrapper), "Strix Sms: 同步删除签名失败.");
            }
            if (addKeys.size() > 0) {
                List<SmsSign> smsSignList = new ArrayList<>();
                addKeys.forEach(k -> {
                    StrixSmsSign strixSmsSign = signList.stream().filter(s -> s.getName().equals(k)).findFirst().get();
                    SmsSign smsSign = new SmsSign();
                    smsSign.setConfigKey(configKey);
                    smsSign.setName(k);
                    smsSign.setStatus(strixSmsSign.getStatus());
                    smsSign.setCreateTime(strixSmsSign.getCreateTime());
                    smsSign.setCreateBy("System");
                    smsSign.setUpdateBy("System");
                    smsSignList.add(smsSign);
                });
                Assert.isTrue(saveBatch(smsSignList), "Strix Sms: 同步增加签名失败.");
            }
        }));
    }
}
