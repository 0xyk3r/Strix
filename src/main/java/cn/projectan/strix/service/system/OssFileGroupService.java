package cn.projectan.strix.service.system;

import cn.projectan.strix.mapper.system.OssFileGroupMapper;
import cn.projectan.strix.model.db.system.OssFileGroup;
import cn.projectan.strix.model.response.common.CommonSelectDataResp;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * <p>
 * Strix OSS 文件组 服务类
 * </p>
 *
 * @author ProjectAn
 * @since 2023-05-22
 */
@Service
public class OssFileGroupService extends ServiceImpl<OssFileGroupMapper, OssFileGroup> {

    /**
     * 根据 groupKey 获取文件组
     *
     * @param groupKey 文件组 key
     * @return 文件组
     */
    public OssFileGroup getGroupByKey(String groupKey) {
        return lambdaQuery()
                .eq(OssFileGroup::getKey, groupKey)
                .one();
    }

    /**
     * 获取下拉数据
     *
     * @return 下拉数据
     */
    public CommonSelectDataResp getSelectData() {
        return getSelectData(null);
    }

    /**
     * 根据 groupKey 获取下拉数据
     *
     * @param configKey 配置 key
     * @return 下拉数据
     */
    public CommonSelectDataResp getSelectData(String configKey) {
        List<OssFileGroup> ossFileGroupList = lambdaQuery()
                .eq(StringUtils.hasText(configKey), OssFileGroup::getConfigKey, configKey)
                .list();
        return new CommonSelectDataResp(ossFileGroupList, "key", "name", null);
    }

}
