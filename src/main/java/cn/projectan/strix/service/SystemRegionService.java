package cn.projectan.strix.service;

import cn.projectan.strix.model.db.SystemRegion;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;
import java.util.Map;

/**
 * <p>
 * 服务类
 * </p>
 *
 * @author ProjectAn
 * @since 2021-09-29
 */
public interface SystemRegionService extends IService<SystemRegion> {

    /**
     * 获取系统地区 （带缓存）
     *
     * @param id 地区id
     * @return 系统地区对象
     */
    SystemRegion queryRegionById(String id);

    /**
     * 获取完整的地区信息（完整id、完整地区名）
     *
     * @param id 地区id
     * @return map对象，包含level、name和path三个key
     */
    Map<String, String> getFullInfo(String id);

    /**
     * 获取所有子节点地区的ID（包括本节点）
     *
     * @param id 地区id
     * @return 所有子节点的地区id集合
     */
    List<String> getChildrenIdList(String id);

    /**
     * 根据完整节点路径获取子地区 (基于全文索引的模糊查询)
     *
     * @param parentFullName 完整节点路径
     * @return 所有子节点的地区id集合
     */
    List<SystemRegion> getMatchChildren(String parentFullName);

    /**
     * 更新关联地区的信息
     *
     * @param systemRegion  系统地区
     * @param newParentId   新的父节点id
     * @param updateWrapper 更新条件构造器
     */
    void updateRelevantRegion(SystemRegion systemRegion, String newParentId, LambdaUpdateWrapper<SystemRegion> updateWrapper);

}
