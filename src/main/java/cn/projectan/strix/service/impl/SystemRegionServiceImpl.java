package cn.projectan.strix.service.impl;

import cn.projectan.strix.core.cache.SystemRegionCache;
import cn.projectan.strix.mapper.SystemRegionMapper;
import cn.projectan.strix.model.db.SystemRegion;
import cn.projectan.strix.service.SystemRegionService;
import cn.projectan.strix.utils.SpringUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import java.util.*;

/**
 * <p>
 * 服务实现类
 * </p>
 *
 * @author ProjectAn
 * @since 2021-09-29
 */
@Service
@RequiredArgsConstructor
public class SystemRegionServiceImpl extends ServiceImpl<SystemRegionMapper, SystemRegion> implements SystemRegionService {

    private final SystemRegionCache systemRegionCache;

    @Cacheable(value = "strix:system:region:queryRegionById", key = "#id")
    @Override
    public SystemRegion queryRegionById(String id) {
        return getBaseMapper().selectById(id);
    }

    @Override
    public Map<String, String> getFullInfo(String id) {
        Map<String, String> result = new HashMap<>();

        SystemRegionService systemRegionService = SpringUtil.getBean(SystemRegionService.class);
        SystemRegion currentRegion = getBaseMapper().selectById(id);

        int level = 1;
        List<String> fullPathList = new ArrayList<>();
        List<String> fullNameList = new ArrayList<>();
        fullPathList.add(currentRegion.getId());
        fullNameList.add(currentRegion.getName());

        while (StringUtils.hasText(currentRegion.getParentId()) && !"0".equals(currentRegion.getParentId())) {
            currentRegion = systemRegionService.queryRegionById(currentRegion.getParentId());

            fullPathList.addFirst(currentRegion.getId());
            fullNameList.addFirst(currentRegion.getName());
            level++;
        }

        String fullPath = String.join(",", fullPathList);
        fullPath = "," + fullPath + ",";
        String fullName = String.join("-", fullNameList);

        result.put("path", fullPath);
        result.put("name", fullName);
        result.put("level", String.valueOf(level));

        return result;
    }

    @Cacheable(value = "strix:system:region:getChildrenIdList", key = "'r_'.concat(#id)")
    @Override
    public List<String> getChildrenIdList(String id) {
        SystemRegion systemRegion = getBaseMapper().selectById(id);

        QueryWrapper<SystemRegion> systemRegionQueryWrapper = new QueryWrapper<>();
        systemRegionQueryWrapper.select("id");
        systemRegionQueryWrapper.likeRight("full_path", systemRegion.getFullPath());
        return this.listObjs(systemRegionQueryWrapper, Object::toString);
    }

    @Override
    public List<SystemRegion> getMatchChildren(String parentFullName) {
        parentFullName = "+\"" + parentFullName + "\"";
        return getBaseMapper().getMatchChildren(parentFullName);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void updateRelevantRegion(SystemRegion systemRegion, String newParentId, UpdateWrapper<SystemRegion> updateWrapper) {
        SystemRegionService systemRegionService = SpringUtil.getBean(SystemRegionService.class);
        // 查询新父级节点的信息
        SystemRegion newParentRegion = systemRegionService.getById(newParentId);
        Assert.notNull(newParentRegion, "父级系统地区信息不存在");
        // 获取被修改的节点的子节点信息（包括自身）
        List<SystemRegion> relevantRegions = systemRegionService.getMatchChildren(systemRegion.getFullPath());
        // 找出子节点信息集合中的自身节点（利用条件level最小查找）
        SystemRegion currRegion = relevantRegions.stream().min(Comparator.comparing(SystemRegion::getLevel)).orElse(null);
        Assert.notNull(currRegion, "原系统地区信息不存在");
        // 构建新节点 fullPath、fullName
        String newCurrRegionPath = newParentRegion.getFullPath() + systemRegion.getId() + ",";
        String newCurrRegionName = newParentRegion.getFullName() + "-" + systemRegion.getName();
        // 新节点 level 字段偏移量
        Integer newLevelOffset = newParentRegion.getLevel() - systemRegion.getLevel() + 1;
        // 遍历修改子节点（包括当前）
        relevantRegions.forEach(r -> {
            r.setFullPath(r.getFullPath().replaceFirst(systemRegion.getFullPath(), newCurrRegionPath));
            r.setFullName(r.getFullName().replaceFirst(systemRegion.getFullName(), newCurrRegionName));
            r.setLevel(r.getLevel() + newLevelOffset);
        });
        // 批量保存
        Assert.isTrue(systemRegionService.updateBatchById(relevantRegions), "保存系统地区相关信息失败");
        // 保存被修改的节点数据（必须在下面，否则会被覆盖）
        Assert.isTrue(systemRegionService.update(updateWrapper), "保存系统地区失败");
        // 取原父节点的 fullPath 遍历刷新缓存
        for (String rid : systemRegion.getFullPath().split(",")) {
            systemRegionCache.refreshRedisCacheById(rid);
        }
        // 获取相关节点中的最下级节点的 fullPath 并刷新缓存
        relevantRegions.stream().max(Comparator.comparing(SystemRegion::getLevel)).map(SystemRegion::getFullPath).ifPresent(rfp -> {
            for (String rid : rfp.split(",")) {
                systemRegionCache.refreshRedisCacheById(rid);
            }
        });

    }

}
