package cn.projectan.strix.service.impl;

import cn.projectan.strix.mapper.SystemRegionMapper;
import cn.projectan.strix.model.db.SystemRegion;
import cn.projectan.strix.service.SystemRegionService;
import cn.projectan.strix.utils.SpringUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * <p>
 * 服务实现类
 * </p>
 *
 * @author 安炯奕
 * @since 2021-09-29
 */
@Service
public class SystemRegionServiceImpl extends ServiceImpl<SystemRegionMapper, SystemRegion> implements SystemRegionService {

    @Cacheable(value = "strix:system:region:queryRegionById", key = "#id")
    @Override
    public SystemRegion queryRegionById(String id) {
        return getBaseMapper().selectById(id);
    }

    @Override
    public Map<String, String> getFullInfo(String id) {
        Map<String, String> result = new HashMap<>();
        // 避免使用this调用同类方法，会导致缓存不生效
        SystemRegionService systemRegionService = SpringUtil.getBean(SystemRegionService.class);

        SystemRegion currentRegion = getBaseMapper().selectById(id);

        int level = 1;
        List<String> fullPathList = new ArrayList<>();
        List<String> fullNameList = new ArrayList<>();
        fullPathList.add(currentRegion.getId());
        fullNameList.add(currentRegion.getName());

        while (StringUtils.hasText(currentRegion.getParentId()) && !"0".equals(currentRegion.getParentId())) {
            currentRegion = systemRegionService.queryRegionById(currentRegion.getParentId());

            fullPathList.add(0, currentRegion.getId());
            fullNameList.add(0, currentRegion.getName());
            level++;
        }

        String fullPath = String.join(",", fullPathList);
        fullPath = "," + fullPath + ",";
        String fullName = String.join("-", fullNameList);

        result.put("path", fullPath);
        result.put("name", fullName);
        result.put("level", level + "");

        return result;
    }

    @Cacheable(value = "strix:system:region:getChildrenIdList", key = "#id")
    @Override
    public List<String> getChildrenIdList(String id) {
        SystemRegion systemRegion = getBaseMapper().selectById(id);

        QueryWrapper<SystemRegion> systemRegionQueryWrapper = new QueryWrapper<>();
        systemRegionQueryWrapper.select("id");
        systemRegionQueryWrapper.likeRight("full_path", systemRegion.getFullPath());
        return this.listObjs(systemRegionQueryWrapper, Object::toString);
    }

}
