package cn.projectan.strix.utils;

import cn.hutool.core.util.RandomUtil;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

/**
 * @author ProjectAn
 * @date 2023/5/22 10:57
 */
class CollectionDiffUtilTest {

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
        for (int i = 0; i < 100000; i++) {
            a.add(RandomUtil.randomInt(1, 666) + "");
        }
        for (int i = 0; i < 100000; i++) {
            b.add(RandomUtil.randomInt(1, 666) + "");
        }
        long start;

        System.out.println("==========================================");
        System.out.println("==========================================");

        System.out.println("----subListMiddle----");
        start = System.nanoTime();
        System.out.println("size: " + CollectionDiffUtil.subListBySet(a, b).size());
        System.out.println(System.nanoTime() - start);
        System.out.println("----subListBig----");
        start = System.nanoTime();
        System.out.println("size: " + CollectionDiffUtil.subListByParallel(a, b).size());
        System.out.println(System.nanoTime() - start);
        System.out.println("----subList----");
        start = System.nanoTime();
        System.out.println("size: " + CollectionDiffUtil.subList(a, b).size());
        System.out.println(System.nanoTime() - start);

        System.out.println("==========================================");
        System.out.println("==========================================");

        System.out.println("----subListMiddle----");
        start = System.nanoTime();
        System.out.println("size: " + CollectionDiffUtil.subListBySet(a, b).size());
        System.out.println(System.nanoTime() - start);
        System.out.println("----subListBig----");
        start = System.nanoTime();
        System.out.println("size: " + CollectionDiffUtil.subListByParallel(a, b).size());
        System.out.println(System.nanoTime() - start);
        System.out.println("----subList----");
        start = System.nanoTime();
        System.out.println("size: " + CollectionDiffUtil.subList(a, b).size());
        System.out.println(System.nanoTime() - start);

        System.out.println("==========================================");
        System.out.println("==========================================");
    }

}
