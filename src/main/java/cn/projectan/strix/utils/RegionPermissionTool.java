package cn.projectan.strix.utils;

import cn.projectan.strix.core.exception.StrixException;
import cn.projectan.strix.model.dict.SystemRoleRegionPermissionType;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 地区权限工具
 *
 * @author ProjectAn
 * @date 2024/8/14 12:19
 */
@Slf4j
public class RegionPermissionTool {

    /**
     * 检查地区权限
     *
     * @param regionId 待检查的地区ID
     */
    public static void check(String regionId) {
        // 超级管理员不受限
        if (SecurityUtils.isSuperAdmin() || !StringUtils.hasText(regionId)) {
            return;
        }
        byte regionPermissionType = SecurityUtils.getLoginInfo().getRegionPermissionType();
        switch (regionPermissionType) {
            case SystemRoleRegionPermissionType.ALL_REGION -> {
                // 无需检查
            }
            case SystemRoleRegionPermissionType.WITH_SUB_REGION -> {
                List<String> regionIdList = SecurityUtils.getManagerRegionIdList();
                if (!regionIdList.contains(regionId)) {
                    throw new StrixException("无权限访问该地区数据");
                }
            }
            case SystemRoleRegionPermissionType.CURR_REGION -> {
                if (!SecurityUtils.getManagerRegionId().equals(regionId)) {
                    throw new StrixException("无权限访问该地区数据");
                }
            }
            default -> throw new StrixException("无效的地区权限类型");
        }
    }

    /**
     * 追加地区权限到查询条件
     *
     * @param field        字段
     * @param queryWrapper 查询条件
     */
    public static void appendRegionPermissionToQueryWrapper(String field, QueryWrapper<?> queryWrapper) {
        // 超级管理员不受限
        if (SecurityUtils.isSuperAdmin()) {
            return;
        }
        byte regionPermissionType = SecurityUtils.getLoginInfo().getRegionPermissionType();
        switch (regionPermissionType) {
            case SystemRoleRegionPermissionType.ALL_REGION -> {
                // 无需检查
            }
            case SystemRoleRegionPermissionType.WITH_SUB_REGION -> {
                List<String> regionIdList = SecurityUtils.getManagerRegionIdList();
                queryWrapper.in(field, regionIdList);
            }
            case SystemRoleRegionPermissionType.CURR_REGION -> {
                String regionId = SecurityUtils.getManagerRegionId();
                // 防止未配置地区ID的情况下, 查询越权
                if (!StringUtils.hasText(regionId)) {
                    regionId = "-1";
                }
                queryWrapper.eq(field, regionId);
            }
            default -> throw new StrixException("无效的地区权限类型");
        }
    }

}
