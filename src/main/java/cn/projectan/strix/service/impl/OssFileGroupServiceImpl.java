package cn.projectan.strix.service.impl;

import cn.projectan.strix.mapper.OssFileGroupMapper;
import cn.projectan.strix.model.db.OssFileGroup;
import cn.projectan.strix.model.response.common.CommonSelectDataResp;
import cn.projectan.strix.service.OssFileGroupService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * <p>
 * 服务实现类
 * </p>
 *
 * @author 安炯奕
 * @since 2023-05-22
 */
@Service
public class OssFileGroupServiceImpl extends ServiceImpl<OssFileGroupMapper, OssFileGroup> implements OssFileGroupService {

    @Override
    public OssFileGroup getGroupByKey(String key) {
        return getOne(new LambdaQueryWrapper<>(OssFileGroup.class).eq(OssFileGroup::getKey, key));
    }

    @Override
    public CommonSelectDataResp getSelectData() {
        return getSelectData(null);
    }

    @Override
    public CommonSelectDataResp getSelectData(String configKey) {
        QueryWrapper<OssFileGroup> queryWrapper = new QueryWrapper<>();
        if (StringUtils.hasText(configKey)) {
            queryWrapper.eq("config_key", configKey);
        }
        List<OssFileGroup> ossFileGroupList = getBaseMapper().selectList(queryWrapper);
        return new CommonSelectDataResp(ossFileGroupList, "key", "name", null);
    }

}
