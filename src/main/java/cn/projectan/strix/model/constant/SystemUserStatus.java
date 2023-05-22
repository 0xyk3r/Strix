package cn.projectan.strix.model.constant;

/**
 * @author 安炯奕
 * @date 2021/8/26 15:17
 */
public interface SystemUserStatus {

    /**
     * 禁止登录
     */
    int BANNED = 0;

    /**
     * 正常
     */
    int NORMAL = 1;

    static boolean valid(int status) {
        return status == BANNED || status == NORMAL;
    }

}
