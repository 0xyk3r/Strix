package cn.projectan.strix.service;

import cn.projectan.strix.model.db.OssConfig;
import cn.projectan.strix.model.response.common.CommonSelectDataResp;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
 * <p>
 * 阿里云OSS配置 服务类
 * </p>
 *
 * @author 安炯奕
 * @since 2021-05-02
 */
public interface OssConfigService extends IService<OssConfig> {

    void createInstance(List<OssConfig> ossConfigList);

    CommonSelectDataResp getSelectData();

}
