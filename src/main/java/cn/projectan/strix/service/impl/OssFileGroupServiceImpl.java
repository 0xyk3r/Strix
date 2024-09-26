package cn.projectan.strix.service.impl;

import cn.projectan.strix.mapper.OssFileGroupMapper;
import cn.projectan.strix.model.db.OssFileGroup;
import cn.projectan.strix.model.response.common.CommonSelectDataResp;
import cn.projectan.strix.service.OssFileGroupService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * <p>
 * 服务实现类
 * </p>
 *
 * @author ProjectAn
 * @since 2023-05-22
 */
@Service
public class OssFileGroupServiceImpl extends ServiceImpl<OssFileGroupMapper, OssFileGroup> implements OssFileGroupService {

    @Override
    public OssFileGroup getGroupByKey(String key) {
        return lambdaQuery()
                .eq(OssFileGroup::getKey, key)
                .one();
    }

    @Override
    public CommonSelectDataResp getSelectData() {
        return getSelectData(null);
    }

    @Override
    public CommonSelectDataResp getSelectData(String configKey) {
        List<OssFileGroup> ossFileGroupList = lambdaQuery()
                .eq(StringUtils.hasText(configKey), OssFileGroup::getConfigKey, configKey)
                .list();
        return new CommonSelectDataResp(ossFileGroupList, "key", "name", null);
    }

}
