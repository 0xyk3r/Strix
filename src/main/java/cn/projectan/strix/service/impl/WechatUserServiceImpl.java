package cn.projectan.strix.service.impl;

import cn.projectan.strix.model.db.WechatUser;
import cn.projectan.strix.mapper.WechatUserMapper;
import cn.projectan.strix.service.WechatUserService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author 安炯奕
 * @since 2021-08-25
 */
@Service
public class WechatUserServiceImpl extends ServiceImpl<WechatUserMapper, WechatUser> implements WechatUserService {

}
