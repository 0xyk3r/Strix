package cn.projectan.strix.service.system;

import cn.projectan.strix.mapper.system.SystemManagerRoleMapper;
import cn.projectan.strix.model.db.system.SystemManagerRole;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * <p>
 * Strix 系统管理人员 角色关系 服务类
 * </p>
 *
 * @author ProjectAn
 * @since 2021-05-12
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SystemManagerRoleService extends ServiceImpl<SystemManagerRoleMapper, SystemManagerRole> {

}
