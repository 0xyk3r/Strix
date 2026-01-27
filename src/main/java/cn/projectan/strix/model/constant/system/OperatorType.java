package cn.projectan.strix.model.constant.system;

/**
 * 操作者类型
 *
 * @author ProjectAn
 * @since 2025-01-17 16:12:03
 */
public interface OperatorType {

    /**
     * 无
     */
    short NONE = 0;

    /**
     * 系统
     */
    short SYSTEM = 1;

    /**
     * 管理人员
     */
    short SYSTEM_MANAGER = 11;

    /**
     * 普通用户
     */
    short SYSTEM_USER = 12;

}
