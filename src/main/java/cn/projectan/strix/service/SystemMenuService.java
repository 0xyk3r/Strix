package cn.projectan.strix.service;

import cn.projectan.strix.model.db.SystemMenu;
import cn.projectan.strix.model.response.common.CommonTreeDataResp;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * <p>
 * 服务类
 * </p>
 *
 * @author ProjectAn
 * @since 2021-05-12
 */
public interface SystemMenuService extends IService<SystemMenu> {

    /**
     * 获取树形数据
     *
     * @return 树形数据
     */
    CommonTreeDataResp getTreeData();

}
