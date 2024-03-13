package cn.projectan.strix.service;

import cn.projectan.strix.model.db.SmsConfig;
import cn.projectan.strix.model.response.common.CommonSelectDataResp;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
 * <p>
 * 服务类
 * </p>
 *
 * @author ProjectAn
 * @since 2021-05-02
 */
public interface SmsConfigService extends IService<SmsConfig> {

    void createInstance(List<SmsConfig> smsConfigList);

    CommonSelectDataResp getSelectData();

}
