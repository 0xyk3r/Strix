package cn.projectan.strix.utils;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 集合差异获取
 *
 * @author ProjectAn
 * @date 2021/7/2 17:35
 */
public class ListDiffUtil {

    /**
     * 阈值 用于判断使用哪种算法 <br>
     * 低于阈值使用 {@link #subListMiddle(Collection, Collection)} 空间占用大 时间占用小<br>
     * 高于阈值使用 {@link #subListBig(Collection, Collection)} 空间占用小 时间占用大
     */
    private static final int THRESHOLD = 20000;

    /**
     * 以 list1 为基准，求 list1 中有的但是 list2 中没有的元素
     * 根据 list1 和 list2 的大小，自动选择合适的算法
     *
     * @param list1 基准列表
     * @param list2 比较列表
     * @return list1 中有的但是 list2 中没有的元素
     */
    public static List<String> subList(Collection<String> list1, Collection<String> list2) {
        if (list1.size() < THRESHOLD || list2.size() < THRESHOLD) {
            return subListMiddle(list1, list2);
        } else {
            return subListBig(list1, list2);
        }
    }

    /**
     * 差集 适用于少量-中等数据量<p>
     * 求List1中有的但是List2中没有的元素<p>
     * 空间换时间降低时间复杂度<p>
     * <p>
     * 时间复杂度:O(n + m)<p>
     * 空间复杂度:O(m + n)
     *
     * @param list1 基准列表
     * @param list2 比较列表
     * @return list1 中有的但是 list2 中没有的元素
     */
    public static List<String> subListMiddle(Collection<String> list1, Collection<String> list2) {
        Set<String> set2 = new HashSet<>(list2);
        return list1.stream()
                .filter(str -> !set2.contains(str))
                .collect(Collectors.toList());
    }

    /**
     * 差集(基于java8新特性)优化解法2 适用于大数据量 <p>
     * 求List1中有的但是List2中没有的元素
     */
    public static List<String> subListBig(Collection<String> list1, Collection<String> list2) {
        Map<String, String> tempMap = list2.parallelStream().collect(Collectors.toMap(Function.identity(), Function.identity(), (oldData, newData) -> newData));
        return list1.parallelStream().filter(str -> !tempMap.containsKey(str)).collect(Collectors.toList());
    }

}
