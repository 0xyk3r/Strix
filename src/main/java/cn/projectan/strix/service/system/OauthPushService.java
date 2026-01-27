package cn.projectan.strix.service.system;

import cn.projectan.strix.mapper.system.OauthPushMapper;
import cn.projectan.strix.model.db.system.OauthPush;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * <p>
 * Strix OAuth 消息推送记录 服务类
 * </p>
 *
 * @author ProjectAn
 * @since 2024-04-08
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OauthPushService extends ServiceImpl<OauthPushMapper, OauthPush> {

}
