package cn.projectan.strix.service.system;

import cn.projectan.strix.mapper.system.SystemRoleMenuMapper;
import cn.projectan.strix.model.db.system.SystemRoleMenu;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 服务类
 * </p>
 *
 * @author ProjectAn
 * @since 2021-06-21
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SystemRoleMenuService extends ServiceImpl<SystemRoleMenuMapper, SystemRoleMenu> {
}
