package cn.projectan.strix.service.system;

import cn.projectan.strix.mapper.system.SystemUserRelationMapper;
import cn.projectan.strix.model.db.system.SystemUserRelation;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * <p>
 * Strix 系统用户 第三方账户绑定关系 服务类
 * </p>
 *
 * @author ProjectAn
 * @since 2021-08-26
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SystemUserRelationService extends ServiceImpl<SystemUserRelationMapper, SystemUserRelation> {

}
