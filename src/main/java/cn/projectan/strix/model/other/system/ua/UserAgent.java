package cn.projectan.strix.model.other.system.ua;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * User-Agent 信息对象
 *
 * @author ProjectAn
 * @since 2024/3/31 02:56
 */
@Data
public class UserAgent implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 是否为移动平台
     */
    private boolean mobile;
    /**
     * 浏览器类型
     */
    private Browser browser;
    /**
     * 浏览器版本
     */
    private String version;

    /**
     * 平台类型
     */
    private Platform platform;

    /**
     * 系统类型
     */
    private OS os;
    /**
     * 系统版本
     */
    private String osVersion;

    /**
     * 引擎类型
     */
    private Engine engine;
    /**
     * 引擎版本
     */
    private String engineVersion;

}
