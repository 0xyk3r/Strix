package cn.projectan.strix.util;

import cn.projectan.strix.util.algo.SameValueFinder;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author ProjectAn
 * @since 2022/10/27 15:28
 */
public class SameValueFinderTest {

    @Data
    @AllArgsConstructor
    static class AAA {
        private String a1;
        private String a2;
        private String a3;
        private String a4;
    }

    @Data
    @AllArgsConstructor
    static class BBB {
        private String b1;
        private String a2;
        private String b3;
        private String a4;
        private String b5;
    }

    @Test
    void test() {
        List<AAA> aList = new ArrayList<>();
        aList.add(new AAA("aa", "bb", "cc", "dd"));
        aList.add(new AAA("aa", "bb", "dd", "ee"));
        aList.add(new AAA("bb", "cc", "dd", "ee"));
        aList.add(new AAA("ff", "fff", "ffff", "fffff"));
        aList.add(new AAA("ff", "fff", "ffff", "ffffff"));
        for (int i = 0; i < 100000; i++) {
            aList.add(new AAA("ff", "fff", "ffff", "fffff"));
            if (i % 99 == 0) {
                aList.add(new AAA("aa", "bb", "cc", "dd"));
                aList.add(new AAA("aa", "bb", "dd", "ee"));
                aList.add(new AAA("bb", "cc", "dd", "ee"));
            }
        }
        List<BBB> bList = new ArrayList<>();
        bList.add(new BBB("bb", "22", "33", "333", "bb"));
        bList.add(new BBB("11", "22", "33", "44", "fff"));
        bList.add(new BBB("asd", "asdf", "asdfg", "asdff", "asdgg"));
        bList.add(new BBB("asd", "asdf", "asdfg", "asdff", "asdgg"));
        for (int i = 0; i < 100000; i++) {
            bList.add(new BBB("asd", "asdf", "asdfg", "asdff", "asdgg"));
            if (i % 88 == 0) {
                bList.add(new BBB("bb", "22", "33", "333", "bb"));
                bList.add(new BBB("11", "22", "33", "44", "fff"));
            }
        }

        System.out.println("aList size: " + aList.size());
        System.out.println("bList size: " + bList.size());

        Set<AAA> fuckCommPre = SameValueFinder.find(aList, bList, "a2", "b5");

        long start2 = System.nanoTime();
        for (int i = 0; i < 10; i++) {
            Set<AAA> fuckComm = SameValueFinder.find(aList, bList, "a2", "b5");
        }
        System.out.println("通用版耗时(反射实现)：\t\t" + (System.nanoTime() - start2) / 100 + "ns");
        System.out.println("find: " + fuckCommPre.size());
//        fuckComm.forEach(System.out::println);

        Set<AAA> fuckLambdaPre = aList.stream().filter(a -> bList.stream().anyMatch(b -> a.getA2().equals(b.getB5()))).collect(Collectors.toSet());

        long start3 = System.nanoTime();
        for (int i = 0; i < 10; i++) {
            Set<AAA> fuckLambda = aList.stream().filter(a -> bList.stream().anyMatch(b -> a.getA2().equals(b.getB5()))).collect(Collectors.toSet());
        }
        System.out.println("Lambda耗时(未封装)：\t\t" + (System.nanoTime() - start3) / 100 + "ns");
        System.out.println("find: " + fuckLambdaPre.size());
//        fuckLambda.forEach(System.out::println);

        long start4 = System.nanoTime();
        Set<AAA> fuckFor = new HashSet<>();
        for (int i = 0; i < 10; i++) {
            for (AAA a : aList) {
                for (BBB b : bList) {
                    if (a.getA2().equals(b.getB5())) {
                        fuckFor.add(a);
                        break;
                    }
                }
            }
        }
        System.out.println("普通for耗时(未封装)：\t\t" + (System.nanoTime() - start4) / 100 + "ns");
        System.out.println("find: " + fuckFor.size());
//        fuckFor.forEach(System.out::println);
    }

}
