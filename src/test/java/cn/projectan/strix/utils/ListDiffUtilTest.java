package cn.projectan.strix.utils;

import cn.hutool.core.util.RandomUtil;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

/**
 * @author 安炯奕
 * @date 2023/5/22 10:57
 */
class ListDiffUtilTest {

    @Test
    void test() {
        List<String> a = new ArrayList<>();
        List<String> b = new ArrayList<>();

        a.add("1");
        a.add("2");

        b.add("1");
        b.add("2");
        b.add("2");
        b.add("3");
        b.add("4");
        for (int i = 0; i < 6666; i++) {
            a.add(RandomUtil.randomInt(1, 666) + "");
            b.add(RandomUtil.randomInt(1, 666) + "");
        }
        long start;

        System.out.println("==========================================");
        System.out.println("==========================================");

        System.out.println("----subListMiddle----");
        start = System.nanoTime();
        System.out.println("size: " + ListDiffUtil.subListMiddle(a, b).size());
        System.out.println(System.nanoTime() - start);
        System.out.println("----subListBig----");
        start = System.nanoTime();
        System.out.println("size: " + ListDiffUtil.subListBig(a, b).size());
        System.out.println(System.nanoTime() - start);
        System.out.println("----subList----");
        start = System.nanoTime();
        System.out.println("size: " + ListDiffUtil.subList(a, b).size());
        System.out.println(System.nanoTime() - start);

        System.out.println("==========================================");
        System.out.println("==========================================");

        System.out.println("----subListMiddle----");
        start = System.nanoTime();
        System.out.println("size: " + ListDiffUtil.subListMiddle(a, b).size());
        System.out.println(System.nanoTime() - start);
        System.out.println("----subListBig----");
        start = System.nanoTime();
        System.out.println("size: " + ListDiffUtil.subListBig(a, b).size());
        System.out.println(System.nanoTime() - start);
        System.out.println("----subList----");
        start = System.nanoTime();
        System.out.println("size: " + ListDiffUtil.subList(a, b).size());
        System.out.println(System.nanoTime() - start);

        System.out.println("==========================================");
        System.out.println("==========================================");
    }

}
