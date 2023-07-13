package cn.projectan.strix.utils;

/**
 * @author 安炯奕
 * @date 2023/6/21 15:27
 */
public class TextUtils {

    public static String camelToUnderline(String camelName) {
        if (camelName == null || camelName.isEmpty()) {
            return camelName;
        }
        StringBuilder sb = new StringBuilder();
        char[] chars = camelName.toCharArray();
        // 第一个字符直接转换为小写
        sb.append(Character.toLowerCase(chars[0]));
        for (int i = 1; i < chars.length; i++) {
            char c = chars[i];
            if (Character.isUpperCase(c)) {
                sb.append("_").append(Character.toLowerCase(c));
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

}
