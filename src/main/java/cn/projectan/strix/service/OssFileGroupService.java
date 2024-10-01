package cn.projectan.strix.service;

import cn.projectan.strix.model.db.OssFileGroup;
import cn.projectan.strix.model.response.common.CommonSelectDataResp;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * <p>
 * 服务类
 * </p>
 *
 * @author ProjectAn
 * @since 2023-05-22
 */
public interface OssFileGroupService extends IService<OssFileGroup> {

    /**
     * 根据groupKey获取文件组
     *
     * @param groupKey groupKey
     * @return 文件组
     */
    OssFileGroup getGroupByKey(String groupKey);

    /**
     * 获取下拉数据
     *
     * @return 下拉数据
     */
    CommonSelectDataResp getSelectData();

    /**
     * 根据groupKey获取下拉数据
     *
     * @param configKey 配置key
     * @return 下拉数据
     */
    CommonSelectDataResp getSelectData(String configKey);

}
