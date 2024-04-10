package cn.projectan.strix.service;

import cn.projectan.strix.model.db.OauthConfig;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
 * <p>
 * Strix OAuth 配置 服务类
 * </p>
 *
 * @author ProjectAn
 * @since 2024-04-03
 */
public interface OauthConfigService extends IService<OauthConfig> {

    /**
     * 创建 OAuth 配置
     *
     * @param oauthConfigList OAuth 配置列表
     */
    void createInstance(List<OauthConfig> oauthConfigList);

}
