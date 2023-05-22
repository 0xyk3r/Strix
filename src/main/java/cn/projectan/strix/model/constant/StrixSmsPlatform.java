package cn.projectan.strix.model.constant;

/**
 * @author 安炯奕
 * @date 2023/5/20 15:15
 */
public interface StrixSmsPlatform {

    int ALIYUN = 1;

    int TENCENT = 2;

    static boolean valid(int platform) {
        return platform == ALIYUN || platform == TENCENT;
    }

}
