package cn.projectan.strix.service;

import cn.projectan.strix.model.db.OssFileGroup;
import cn.projectan.strix.model.response.common.CommonSelectDataResp;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * <p>
 * 服务类
 * </p>
 *
 * @author 安炯奕
 * @since 2023-05-22
 */
public interface OssFileGroupService extends IService<OssFileGroup> {

    OssFileGroup getGroupByKey(String groupKey);

    CommonSelectDataResp getSelectData();

    CommonSelectDataResp getSelectData(String configKey);

}
