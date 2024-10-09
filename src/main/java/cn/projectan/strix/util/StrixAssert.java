package cn.projectan.strix.util;

import org.springframework.util.Assert;

import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * 断言工具类
 *
 * @author ProjectAn
 * @since 2021/6/16 15:34
 */
public class StrixAssert {

    public static void in(String checkValue, String message, String... passValue) {
        Stream.of(passValue)
                .filter(pass -> pass.equals(checkValue))
                .findAny()
                .orElseThrow(() -> new IllegalArgumentException(message));
    }

    public static void in(String checkValue, String message, int... passValue) {
        Assert.hasText(checkValue, message);
        int cv = Integer.parseInt(checkValue);
        IntStream.of(passValue)
                .filter(pass -> pass == cv)
                .findAny()
                .orElseThrow(() -> new IllegalArgumentException(message));
    }

    public static void in(Integer checkValue, String message, int... passValue) {
        Assert.notNull(checkValue, message);
        IntStream.of(passValue)
                .filter(pass -> pass == checkValue)
                .findAny()
                .orElseThrow(() -> new IllegalArgumentException(message));
    }

}
