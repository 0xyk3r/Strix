package cn.projectan.strix.model.constant;

/**
 * 系统管理用户 类型
 *
 * @author 安炯奕
 * @date 2021/6/16 15:32
 */
public interface SystemManagerType {

    int SUPER_ACCOUNT = 1;

    int PLATFORM_ACCOUNT = 2;

    static boolean valid(int type) {
        return type == SUPER_ACCOUNT || type == PLATFORM_ACCOUNT;
    }

}
