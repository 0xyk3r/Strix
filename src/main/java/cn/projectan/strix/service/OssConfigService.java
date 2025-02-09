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
 * @author ProjectAn
 * @since 2021-05-02
 */
public interface OssConfigService extends IService<OssConfig> {

    void refreshConfig();

    /**
     * 创建实例
     *
     * @param ossConfigList 阿里云OSS配置列表
     */
    void createInstance(List<OssConfig> ossConfigList);

    /**
     * 获取下拉数据
     *
     * @return 下拉数据
     */
    CommonSelectDataResp getSelectData();

}
