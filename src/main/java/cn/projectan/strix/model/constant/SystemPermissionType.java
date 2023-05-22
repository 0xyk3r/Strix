package cn.projectan.strix.model.constant;

/**
 * 系统权限 权限类型
 *
 * @author 安炯奕
 * @date 2021/7/20 15:48
 */
public interface SystemPermissionType {

    int READ_ONLY = 1;

    int READ_WRITE = 2;

    static boolean valid(int type) {
        return type == READ_ONLY || type == READ_WRITE;
    }

}
