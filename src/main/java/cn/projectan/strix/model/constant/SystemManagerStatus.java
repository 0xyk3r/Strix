package cn.projectan.strix.model.constant;

/**
 * 系统管理用户 状态
 *
 * @author 安炯奕
 * @date 2021/5/12 18:52
 */
public interface SystemManagerStatus {

    /**
     * 禁止登录
     */
    int BANNED = 0;

    /**
     * 正常
     */
    int NORMAL = 1;

    static boolean valid(Integer status) {
        return status != null && (status == BANNED || status == NORMAL);
    }

}
