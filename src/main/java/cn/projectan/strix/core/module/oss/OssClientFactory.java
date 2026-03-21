package cn.projectan.strix.core.module.oss;

import cn.projectan.strix.model.db.system.OssConfig;

/**
 * OSS 客户端工厂接口
 * <p>
 * 实现此接口并注册为 Spring Bean 即可自动支持新的对象存储平台，
 * 无需修改 OssConfigService 代码。
 *
 * @author ProjectAn
 */
public interface OssClientFactory {

    /**
     * 该工厂支持的平台标识
     *
     * @return 平台标识，对应 {@link cn.projectan.strix.model.dict.system.OssPlatform} 中的常量
     */
    short supportedPlatform();

    /**
     * 根据配置创建对象存储客户端实例
     *
     * @param config OSS 配置
     * @return OSS 客户端实例
     * @throws Exception 创建失败时抛出异常
     */
    StrixOssClient createClient(OssConfig config) throws Exception;

}
