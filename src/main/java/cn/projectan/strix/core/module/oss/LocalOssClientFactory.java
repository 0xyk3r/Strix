package cn.projectan.strix.core.module.oss;

import cn.projectan.strix.model.db.system.OssConfig;
import cn.projectan.strix.model.dict.system.OssPlatform;
import org.springframework.stereotype.Component;

/**
 * 本地存储客户端工厂
 *
 * @author ProjectAn
 */
@Component
public class LocalOssClientFactory implements OssClientFactory {

    @Override
    public short supportedPlatform() {
        return OssPlatform.LOCAL;
    }

    @Override
    public StrixOssClient createClient(OssConfig config) {
        return new LocalOssClient(config.getPublicEndpoint(), config.getPrivateEndpoint());
    }

}
