package cn.projectan.strix.service.impl;

import cn.projectan.strix.mapper.SystemRoleMenuMapper;
import cn.projectan.strix.model.db.SystemMenu;
import cn.projectan.strix.model.db.SystemRoleMenu;
import cn.projectan.strix.service.SystemMenuService;
import cn.projectan.strix.service.SystemRoleMenuService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.List;

/**
 * <p>
 * 服务实现类
 * </p>
 *
 * @author 安炯奕
 * @since 2021-06-21
 */
@Service
public class SystemRoleMenuServiceImpl extends ServiceImpl<SystemRoleMenuMapper, SystemRoleMenu> implements SystemRoleMenuService {
}
