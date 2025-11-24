package cn.projectan.strix.service;

import cn.projectan.strix.model.db.SystemPermission;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.Collection;

/**
 * <p>
 * 服务类
 * </p>
 *
 * @author ProjectAn
 * @since 2021-05-12
 */
public interface SystemPermissionService extends IService<SystemPermission> {

    void deleteByIds(Collection<String> idList);

}
