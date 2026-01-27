package cn.projectan.strix.service.system;

import cn.projectan.strix.core.cache.system.SystemRegionCache;
import cn.projectan.strix.mapper.system.SystemRegionMapper;
import cn.projectan.strix.model.db.system.SystemRegion;
import cn.projectan.strix.util.common.SpringUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * <p>
 * Strix 系统地区 服务类
 * </p>
 *
 * @author ProjectAn
 * @since 2021-09-29
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SystemRegionService extends ServiceImpl<SystemRegionMapper, SystemRegion> {

    public static final String ROOT_PARENT_ID = "0";
    public static final String PATH_SEPARATOR = ",";
    public static final String NAME_SEPARATOR = "-";

    private final SystemRegionCache systemRegionCache;

    /**
     * 获取系统地区 （带缓存）
     *
     * @param id 地区id
     * @return 系统地区对象
     */
    @Cacheable(value = "strix:system:region:queryRegionById", key = "#id")
    public SystemRegion queryRegionById(String id) {
        return getBaseMapper().selectById(id);
    }

    /**
     * 获取完整的地区信息（完整id、完整地区名）
     *
     * @param id 地区id
     * @return map对象，包含level、name和path三个key
     */
    public Map<String, String> getFullInfo(String id) {
        SystemRegionService proxy = SpringUtil.getAopProxy(this);
        SystemRegion region = getBaseMapper().selectById(id);
        Assert.notNull(region, "地区信息不存在");

        List<String> pathList = new ArrayList<>();
        List<String> nameList = new ArrayList<>();
        int level = 1;

        pathList.add(region.getId());
        nameList.add(region.getName());

        while (StringUtils.hasText(region.getParentId()) && !ROOT_PARENT_ID.equals(region.getParentId())) {
            region = proxy.queryRegionById(region.getParentId());
            pathList.addFirst(region.getId());
            nameList.addFirst(region.getName());
            level++;
        }

        return Map.of(
                "path", PATH_SEPARATOR + String.join(PATH_SEPARATOR, pathList) + PATH_SEPARATOR,
                "name", String.join(NAME_SEPARATOR, nameList),
                "level", String.valueOf(level)
        );
    }

    /**
     * 获取所有子节点地区的ID（包括本节点）
     *
     * @param id 地区id
     * @return 所有子节点的地区id集合
     */
    @Cacheable(value = "strix:system:region:getChildrenIdList", key = "#id")
    public List<String> getChildrenIdList(String id) {
        SystemRegion systemRegion = getBaseMapper().selectById(id);
        Assert.notNull(systemRegion, "地区信息不存在");

        return lambdaQuery()
                .select(SystemRegion::getId)
                .likeRight(SystemRegion::getFullPath, systemRegion.getFullPath())
                .list()
                .stream()
                .map(SystemRegion::getId)
                .collect(Collectors.toList());
    }

    /**
     * 根据完整节点路径获取子地区（包括自身）
     *
     * @param fullPath 完整节点路径
     * @return 该节点及所有子节点的地区集合
     */
    public List<SystemRegion> getChildrenByFullPath(String fullPath) {
        return lambdaQuery()
                .likeRight(SystemRegion::getFullPath, fullPath)
                .list();
    }

    /**
     * 更新地区名称及其子地区的完整名称
     *
     * @param systemRegion 系统地区（包含旧数据）
     * @param newName      新的地区名称
     */
    @Transactional(rollbackFor = Exception.class)
    public void updateRegionName(SystemRegion systemRegion, String newName) {
        // 构建旧的和新的 fullName 前缀
        String oldFullName = systemRegion.getFullName();
        String newFullName;

        // 如果是顶级地区（没有父节点），fullName 就是名称本身
        if (!StringUtils.hasText(systemRegion.getParentId()) || ROOT_PARENT_ID.equals(systemRegion.getParentId())) {
            newFullName = newName;
        } else {
            newFullName = oldFullName.substring(0, oldFullName.lastIndexOf(NAME_SEPARATOR) + 1) + newName;
        }

        // 获取该节点及其子节点（基于 fullPath 前缀匹配）
        List<SystemRegion> relevantRegions = getChildrenByFullPath(systemRegion.getFullPath());

        // 遍历修改子节点（包括当前节点）的 fullName 和 name
        for (SystemRegion r : relevantRegions) {
            r.setFullName(r.getFullName().replaceFirst(java.util.regex.Pattern.quote(oldFullName), newFullName));
            // 如果是当前节点，还需要更新 name 字段
            if (r.getId().equals(systemRegion.getId())) {
                r.setName(newName);
            }
        }

        // 同步更新传入的 systemRegion 对象（供调用方后续使用）
        systemRegion.setName(newName);
        systemRegion.setFullName(newFullName);

        // 批量保存（包含当前节点和子节点）
        Assert.isTrue(updateBatchById(relevantRegions), "保存系统地区相关信息失败");

        // 刷新缓存
        refreshRelevantRegionCache(systemRegion, relevantRegions);
    }

    /**
     * 更新关联地区的信息（父节点变更时调用）
     *
     * @param systemRegion 系统地区（包含旧数据，name 可能已被修改为新值）
     * @param oldFullPath  旧的完整路径
     * @param oldFullName  旧的完整名称
     * @param newParentId  新的父节点id
     */
    @Transactional(rollbackFor = Exception.class)
    public void updateRelevantRegion(SystemRegion systemRegion, String oldFullPath, String oldFullName, String newParentId) {
        // 查询新父级节点的信息
        SystemRegion newParentRegion = getById(newParentId);
        // 特殊处理：如果新父节点是根节点（"0"），构建虚拟的根节点信息
        if (ROOT_PARENT_ID.equals(newParentId)) {
            newParentRegion = new SystemRegion();
            newParentRegion.setFullPath(PATH_SEPARATOR);
            newParentRegion.setFullName("");
            newParentRegion.setLevel(0);
        } else {
            Assert.notNull(newParentRegion, "父级系统地区信息不存在");
        }

        // 获取被修改的节点的子节点信息（包括自身）- 使用旧的 fullPath 来查询
        List<SystemRegion> relevantRegions = getChildrenByFullPath(oldFullPath);

        // 构建新节点 fullPath、fullName
        String newCurrRegionPath = newParentRegion.getFullPath() + systemRegion.getId() + PATH_SEPARATOR;
        String newCurrRegionName = StringUtils.hasText(newParentRegion.getFullName())
                ? newParentRegion.getFullName() + NAME_SEPARATOR + systemRegion.getName()
                : systemRegion.getName();
        Integer oldLevel = (int) (oldFullPath.chars().filter(c -> c == ',').count() - 1);
        Integer newLevelOffset = newParentRegion.getLevel() - oldLevel + 1;

        // 遍历修改子节点（包括当前）
        for (SystemRegion r : relevantRegions) {
            r.setFullPath(r.getFullPath().replaceFirst(java.util.regex.Pattern.quote(oldFullPath), newCurrRegionPath));
            r.setFullName(r.getFullName().replaceFirst(java.util.regex.Pattern.quote(oldFullName), newCurrRegionName));
            r.setLevel(r.getLevel() + newLevelOffset);
            // 如果是当前节点，还需要更新 parentId
            if (r.getId().equals(systemRegion.getId())) {
                r.setParentId(newParentId);
            }
        }

        // 同步更新传入的 systemRegion 对象（供调用方后续使用）
        systemRegion.setParentId(newParentId);
        systemRegion.setFullPath(newCurrRegionPath);
        systemRegion.setFullName(newCurrRegionName);
        systemRegion.setLevel(newParentRegion.getLevel() + 1);

        // 批量保存
        Assert.isTrue(updateBatchById(relevantRegions), "保存系统地区相关信息失败");

        // 刷新缓存 - 包括旧父节点路径上的所有节点
        Set<String> cacheIds = Stream.of(oldFullPath.split(PATH_SEPARATOR))
                .filter(StringUtils::hasText)
                .collect(Collectors.toSet());
        // 获取相关节点中的最下级节点的 fullPath 刷新缓存
        relevantRegions.stream()
                .max(Comparator.comparing(SystemRegion::getLevel))
                .map(SystemRegion::getFullPath)
                .ifPresent(path -> cacheIds.addAll(Arrays.asList(path.split(PATH_SEPARATOR))));
        // 新父节点也需要刷新缓存
        if (!ROOT_PARENT_ID.equals(newParentId)) {
            cacheIds.add(newParentId);
        }
        cacheIds.forEach(systemRegionCache::refreshRedisCacheById);
    }

    /**
     * 刷新相关地区的缓存
     *
     * @param systemRegion    当前地区
     * @param relevantRegions 相关地区列表
     */
    private void refreshRelevantRegionCache(SystemRegion systemRegion, List<SystemRegion> relevantRegions) {
        Set<String> cacheIds = new HashSet<>();
        // 当前节点路径上的所有节点
        if (StringUtils.hasText(systemRegion.getFullPath())) {
            cacheIds.addAll(Arrays.asList(systemRegion.getFullPath().split(PATH_SEPARATOR)));
        }
        // 所有相关节点
        relevantRegions.forEach(r -> cacheIds.add(r.getId()));
        // 父节点
        if (StringUtils.hasText(systemRegion.getParentId()) && !ROOT_PARENT_ID.equals(systemRegion.getParentId())) {
            cacheIds.add(systemRegion.getParentId());
        }
        cacheIds.stream().filter(StringUtils::hasText).forEach(systemRegionCache::refreshRedisCacheById);
    }

    /**
     * 更新地区基本信息（不涉及层级关系变更）
     *
     * @param systemRegion 系统地区
     */
    @Transactional(rollbackFor = Exception.class)
    public void updateBasicInfo(SystemRegion systemRegion) {
        Assert.isTrue(updateById(systemRegion), "保存系统地区失败");
        systemRegionCache.refreshRedisCacheById(systemRegion.getId());
        if (StringUtils.hasText(systemRegion.getParentId()) && !ROOT_PARENT_ID.equals(systemRegion.getParentId())) {
            systemRegionCache.refreshRedisCacheById(systemRegion.getParentId());
        }
    }

}
