package cn.projectan.strix.utils;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.lang.reflect.Method;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author 安炯奕
 * @date 2022/10/27 15:28
 */
public class FindSameValueInObjectListTest {

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

    private static final String GETTER_PREFIX = "get";

    public static <T> String getField(T bean, String fieldName) {
        try {
            Class<?> clazz = bean.getClass();
            Method getter = clazz.getMethod(GETTER_PREFIX + upperFirst(fieldName));
            return getter.invoke(bean).toString();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static <A, B> Set<A> fuckComm(List<A> aList, List<B> bList, String aFieldName, String bFieldName) {
        Set<A> result = new HashSet<>();

        try {
            Map<String, List<A>> aMap = aList.stream().collect(Collectors.groupingBy(A -> getField(A, aFieldName)));
            Set<String> bSet = bList.stream().map(B -> getField(B, bFieldName)).collect(Collectors.toSet());

            aMap.forEach((k, v) -> {
                if (bSet.contains(k)) {
                    result.addAll(v);
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }

        return result;
    }

    public static Set<AAA> fuck(List<AAA> aList, List<BBB> bList) {
        Set<AAA> result = new HashSet<>();

        Map<String, List<AAA>> aMap = aList.stream().collect(Collectors.groupingBy(AAA::getA2));
        Set<String> bSet = bList.stream().map(BBB::getB5).collect(Collectors.toSet());

        aMap.forEach((k, v) -> {
            if (bSet.contains(k)) {
                result.addAll(v);
            }
        });

        return result;
    }

    public static void main(String[] args) {
        List<AAA> aList = new ArrayList<>();
        aList.add(new AAA("aa", "bb", "cc", "dd"));
        aList.add(new AAA("aa", "bb", "dd", "ee"));
        aList.add(new AAA("bb", "cc", "dd", "ee"));
        aList.add(new AAA("ff", "fff", "ffff", "fffff"));
        aList.add(new AAA("ff", "fff", "ffff", "ffffff"));
        for (int i = 0; i < 1000; i++) {
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
        for (int i = 0; i < 1000; i++) {
            bList.add(new BBB("asd", "asdf", "asdfg", "asdff", "asdgg"));
            if (i % 88 == 0) {
                bList.add(new BBB("bb", "22", "33", "333", "bb"));
                bList.add(new BBB("11", "22", "33", "44", "fff"));
            }
        }

        System.out.println("aList size: " + aList.size());
        System.out.println("bList size: " + bList.size());

        Set<AAA> fuckPre = fuck(aList, bList);

        long start1 = System.nanoTime();
        for (int i = 0; i < 10; i++) {
            Set<AAA> fuck = fuck(aList, bList);
        }
        System.out.println("专用版耗时(未封装)：\t\t" + (System.nanoTime() - start1) / 100 + "ns");
        System.out.println("find: " + fuckPre.size());
//        System.out.println("find: " + fuck.size());
//        fuck.forEach(System.out::println);

        Set<AAA> fuckCommPre = fuckComm(aList, bList, "a2", "b5");

        long start2 = System.nanoTime();
        for (int i = 0; i < 10; i++) {
            Set<AAA> fuckComm = fuckComm(aList, bList, "a2", "b5");
        }
        System.out.println("通用版耗时(反射实现)：\t\t" + (System.nanoTime() - start2) / 100 + "ns");
        System.out.println("find: " + fuckCommPre.size());
//        System.out.println("find: " + fuckComm.size());
//        fuckComm.forEach(System.out::println);

        Set<AAA> fuckLambdaPre = aList.stream().filter(a -> bList.stream().anyMatch(b -> a.getA2().equals(b.getB5()))).collect(Collectors.toSet());

        long start3 = System.nanoTime();
        for (int i = 0; i < 10; i++) {
            Set<AAA> fuckLambda = aList.stream().filter(a -> bList.stream().anyMatch(b -> a.getA2().equals(b.getB5()))).collect(Collectors.toSet());
        }
        System.out.println("Lambda耗时(未封装)：\t\t" + (System.nanoTime() - start3) / 100 + "ns");
        System.out.println("find: " + fuckLambdaPre.size());
//        System.out.println("find: " + fuckLambda.size());
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

    private static String upperFirst(String str) {
        byte[] items = str.getBytes();
        items[0] = (byte) ((char) items[0] - 'a' + 'A');
        return new String(items);
    }

}
