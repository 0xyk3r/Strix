package cn.projectan.strix.model.constant;

/**
 * @author 安炯奕
 * @date 2023/5/28 22:15
 */
public interface DictStatus {

    int ENABLE = 1;

    int DISABLE = 2;

    static boolean valid(int value) {
        return value == ENABLE || value == DISABLE;
    }

}
