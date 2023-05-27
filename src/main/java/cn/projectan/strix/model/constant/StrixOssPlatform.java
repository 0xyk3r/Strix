package cn.projectan.strix.model.constant;

/**
 * @author 安炯奕
 * @date 2023/5/22 15:50
 */
public interface StrixOssPlatform {

    int ALIYUN = 1;

    int TENCENT = 2;

    static boolean valid(int platform) {
        return platform == ALIYUN || platform == TENCENT;
    }

}
