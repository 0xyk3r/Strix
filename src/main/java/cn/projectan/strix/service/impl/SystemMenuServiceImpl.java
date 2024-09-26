package cn.projectan.strix.service.impl;

import cn.projectan.strix.mapper.SystemMenuMapper;
import cn.projectan.strix.model.db.SystemMenu;
import cn.projectan.strix.model.response.common.CommonTreeDataResp;
import cn.projectan.strix.service.SystemMenuService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * <p>
 * 服务实现类
 * </p>
 *
 * @author ProjectAn
 * @since 2021-05-12
 */
@Service
public class SystemMenuServiceImpl extends ServiceImpl<SystemMenuMapper, SystemMenu> implements SystemMenuService {

    @Override
    public CommonTreeDataResp getTreeData() {
        List<SystemMenu> systemMenuList = lambdaQuery()
                .orderByAsc(SystemMenu::getSortValue)
                .list();
        return new CommonTreeDataResp(systemMenuList, "id", "name", "parentId", "0");
    }

}
