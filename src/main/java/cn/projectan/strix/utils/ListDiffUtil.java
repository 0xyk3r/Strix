package cn.projectan.strix.utils;

import cn.hutool.core.util.RandomUtil;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * List差异获取
 *
 * @author 安炯奕
 * @date 2021/7/2 17:35
 */
public class ListDiffUtil {

    public static List<String> subList(List<String> list1, List<String> list2) {
        int size = list1.size() + list2.size();

        if (size < 15000) {
            return subListMiddle(list1, list2);
        } else {
            return subListBig(list1, list2);
        }
    }

    /**
     * 差集(基于常规解法）优化解法1 适用于中等数据量
     * 求List1中有的但是List2中没有的元素
     * 空间换时间降低时间复杂度
     * 时间复杂度O(Max(list1.size(),list2.size()))
     */
    public static List<String> subListMiddle(List<String> list1, List<String> list2) {
        // 空间换时间 降低时间复杂度
        Map<String, String> tempMap = new HashMap<>();
        for (String str : list2) {
            tempMap.put(str, str);
        }
        // LinkedList 频繁添加删除 也可以ArrayList容量初始化为List1.size(),防止数据量过大时频繁扩容以及数组复制
        List<String> resList = new LinkedList<>();
        for (String str : list1) {
            if (!tempMap.containsKey(str)) {
                resList.add(str);
            }
        }
        return resList;
    }

    /**
     * 差集(基于java8新特性)优化解法2 适用于大数据量
     * 求List1中有的但是List2中没有的元素
     */
    public static List<String> subListBig(List<String> list1, List<String> list2) {
        Map<String, String> tempMap = list2.parallelStream().collect(Collectors.toMap(Function.identity(), Function.identity(), (oldData, newData) -> newData));
        return list1.parallelStream().filter(str -> !tempMap.containsKey(str)).collect(Collectors.toList());
    }

    public static void main(String[] args) {
        List<String> a = new ArrayList<>();
        List<String> b = new ArrayList<>();

        for (int i = 0; i < 666; i++) {
            a.add(RandomUtil.randomInt(1, 666) + "");
            b.add(RandomUtil.randomInt(1, 666) + "");
        }
        long start;

//        System.out.println("----Plan One----");
//        start = System.currentTimeMillis();
//        System.out.println("size: " + subListSmall(a, b).size());
//        System.out.println(System.currentTimeMillis() - start);
//        System.out.println("----Plan Two----");
//        start = System.currentTimeMillis();
//        System.out.println("size: " + subListMiddle(a, b).size());
//        System.out.println(System.currentTimeMillis() - start);
//        System.out.println("----Plan Three----");
//        start = System.currentTimeMillis();
//        System.out.println("size: " + subListBig(a, b).size());
//        System.out.println(System.currentTimeMillis() - start);
        System.out.println("----Plan Final----");
        start = System.currentTimeMillis();
        System.out.println("size: " + subList(a, b).size());
        System.out.println(System.currentTimeMillis() - start);

    }

}
