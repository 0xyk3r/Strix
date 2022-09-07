package cn.projectan.strix.utils;

import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.List;

/**
 * 断言工具类
 *
 * @author 安炯奕
 * @date 2021/6/16 15:34
 */
public class StrixAssert {

    public static void in(String checkValue, String message, String... passValue) {
        List<String> passList = Arrays.asList(passValue);
        if (!StringUtils.hasText(checkValue) || !passList.contains(checkValue)) {
            throw new IllegalArgumentException(message);
        }
    }

    public static void in(String checkValue, String message, int... passValue) {
        if (!StringUtils.hasText(checkValue)) {
            throw new IllegalArgumentException(message);
        }
        int cv = Integer.parseInt(checkValue);
        boolean found = false;
        for (int v : passValue) {
            if (cv == v) {
                found = true;
            }
        }
        if (!found) {
            throw new IllegalArgumentException(message);
        }
    }

    public static void in(Integer checkValue, String message, int... passValue) {
        if (checkValue == null) {
            throw new IllegalArgumentException(message);
        }
        boolean found = false;
        for (int v : passValue) {
            if (checkValue == v) {
                found = true;
            }
        }
        if (!found) {
            throw new IllegalArgumentException(message);
        }
    }

}
