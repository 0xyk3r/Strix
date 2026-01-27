package cn.projectan.strix.service.system;

import cn.projectan.strix.mapper.system.SecurityUrlMapper;
import cn.projectan.strix.model.db.system.SecurityUrl;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * <p>
 * Strix 安全 URL 配置 服务类
 * </p>
 *
 * @author ProjectAn
 * @since 2023-04-06
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SecurityUrlService extends ServiceImpl<SecurityUrlMapper, SecurityUrl> {

}
