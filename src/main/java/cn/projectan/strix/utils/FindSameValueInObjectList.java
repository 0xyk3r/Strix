package cn.projectan.strix.utils;

import lombok.extern.slf4j.Slf4j;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 在两个对象集合中查找两个字段相同的对象集合
 * <p>备注：如果性能要求较高，建议参照本工具类自行实现，因为本工具类使用了反射机制，性能较差
 *
 * @author ProjectAn
 * @date 2022/10/27 17:48
 */
@Slf4j
public class FindSameValueInObjectList {

    /**
     * 在两个对象集合中查找两个字段相同的对象集合
     * 返回第一个集合中符合条件（不同类中的不同字段相等）的对象组成的集合
     * <p>
     * e.g.
     * [A{a1:"aaa", a2: "bbb"}]...
     * [B{b1:"aaa", b2: "aaa"}]...
     * 找出所有 A对象中a1的值 等于 B对象中b2的值 的A对象集合
     *
     * @param aList      对象集合A (最终返回的类型)
     * @param bList      对象集合B
     * @param aFieldName 集合A中对象的字段名称
     * @param bFieldName 集合B中对象的字段名称
     * @param <A>        对象A泛型
     * @param <B>        对象B泛型
     * @return 两个对象集合中查找两个字段相同的对象集合
     */
    public static <A, B> Set<A> find(List<A> aList, List<B> bList, String aFieldName, String bFieldName) {
        Set<A> result = new HashSet<>();

        try {
            Map<String, List<A>> aMap = aList.stream().collect(Collectors.groupingBy(A -> ReflectUtil.getString(A, aFieldName)));
            Set<String> bSet = bList.stream().map(B -> ReflectUtil.getString(B, bFieldName)).collect(Collectors.toSet());

            aMap.forEach((k, v) -> {
                if (bSet.contains(k)) {
                    result.addAll(v);
                }
            });
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return null;
        }

        return result;
    }

}
