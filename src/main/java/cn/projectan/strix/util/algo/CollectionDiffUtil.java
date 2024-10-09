package cn.projectan.strix.util.algo;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 集合差异处理器
 *
 * @author ProjectAn
 * @since 2021/7/2 17:35
 */
public class CollectionDiffUtil {

    /**
     * 阈值 用于判断使用哪种算法
     * <p>低于阈值使用 {@link #subListBySet(Collection, Collection) subListMiddle} 空间占用大 时间占用小
     * <p>高于阈值使用 {@link #subListByParallel(Collection, Collection) subListBig} 空间占用小 时间占用大
     */
    private static final int THRESHOLD = 60000;

    /**
     * 以 c1 为基准，求 c1 中存在, 且 c2 中不存在的元素
     * 根据 c1 和 c2 的数据量，自动选择合适的算法
     *
     * @param c1 基准列表
     * @param c2 比较列表
     * @return c1 中有的但是 c2 中没有的元素
     */
    public static List<String> subList(Collection<String> c1, Collection<String> c2) {
        if (c1.size() < THRESHOLD || c2.size() < THRESHOLD) {
            return subListBySet(c1, c2);
        } else {
            return subListByParallel(c1, c2);
        }
    }

    /**
     * 差集 适用于少量-中等数据量
     * <p>求 c1 中有的但是 c2 中没有的元素
     * <p>空间换时间降低时间复杂度
     *
     * @param c1 基准列表
     * @param c2 比较列表
     * @return c1 中有的但是 c2 中没有的元素
     */
    public static List<String> subListBySet(Collection<String> c1, Collection<String> c2) {
        Set<String> set2 = new HashSet<>(c2);
        return c1.stream()
                .filter(str -> !set2.contains(str))
                .collect(Collectors.toList());
    }

    /**
     * 差集 适用于大数据量
     * <p>求 c1 中有的但是 c2 中没有的元素
     * <p>使用并行流提高性能
     *
     * @param c1 基准列表
     * @param c2 比较列表
     * @return c1 中有的但是 c2 中没有的元素
     */
    public static List<String> subListByParallel(Collection<String> c1, Collection<String> c2) {
        Map<String, String> tempMap = c2.parallelStream().collect(Collectors.toMap(Function.identity(), Function.identity(), (oldData, newData) -> newData));
        return c1.parallelStream().filter(str -> !tempMap.containsKey(str)).collect(Collectors.toList());
    }

}
