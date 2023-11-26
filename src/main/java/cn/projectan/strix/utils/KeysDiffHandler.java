package cn.projectan.strix.utils;

import lombok.extern.slf4j.Slf4j;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 集合差异处理器
 *
 * @author 安炯奕
 * @date 2021/7/2 17:21
 */
@Slf4j
public class KeysDiffHandler {

    /**
     * 获取关系Id列表差异 并返回差异Map <br>
     * 更推荐使用 {@link #handle(Collection, Collection, HandleFunction, HandleFunction)} 方法
     *
     * @param oldKeys 旧的关系Id列表
     * @param newKeys 新的关系Id列表
     * @return 返回差异列表 包含remove和add两个key
     */
    public static Map<String, List<String>> handle(Collection<String> oldKeys, Collection<String> newKeys) {
        Map<String, List<String>> result = new HashMap<>();
        try {
            List<String> removeKeys = ListDiffUtil.subList(oldKeys, newKeys);
            List<String> addKeys = ListDiffUtil.subList(newKeys, oldKeys);

            // 过滤内容为空的
            addKeys = addKeys.stream().filter(StringUtils::hasText).collect(Collectors.toList());

            result.put("remove", removeKeys);
            result.put("add", addKeys);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
        return result;
    }

    /**
     * 处理关系Id列表差异 并执行处理函数 <br>
     * 更推荐使用 {@link #handle(Collection, Collection, HandleFunction, HandleFunction)} 方法
     *
     * @param oldKeys 旧的关系Id列表
     * @param newKeys 新的关系Id列表
     * @param func    处理函数 (removeKeys, addKeys)=>{ ... }
     */
    public static void handle(Collection<String> oldKeys, Collection<String> newKeys, FullHandleFunction func) {
        Map<String, List<String>> result = new HashMap<>();
        try {
            List<String> removeKeys = ListDiffUtil.subList(oldKeys, newKeys);
            List<String> addKeys = ListDiffUtil.subList(newKeys, oldKeys);

            // 过滤内容为空的
            addKeys = addKeys.stream().filter(StringUtils::hasText).collect(Collectors.toList());

            if (!removeKeys.isEmpty() || !addKeys.isEmpty()) {
                func.apply(removeKeys, addKeys);
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }

    /**
     * 处理关系Id列表差异 并执行处理函数 <br>
     * 推荐使用 如果需要移除的keys为空 则不会执行移除函数 <br>
     *
     * @param oldKeys    旧的关系Id列表
     * @param newKeys    新的关系Id列表
     * @param removeFunc 处理需要移除的keys函数 (removeKeys)=>{ ... }
     * @param addFunc    处理需要增加的keys函数 (addKeys)=>{ ... }
     */
    public static void handle(Collection<String> oldKeys, Collection<String> newKeys, HandleFunction removeFunc, HandleFunction addFunc) {
        Map<String, List<String>> result = new HashMap<>();
        try {
            List<String> removeKeys = ListDiffUtil.subList(oldKeys, newKeys);
            List<String> addKeys = ListDiffUtil.subList(newKeys, oldKeys);

            Optional.ofNullable(removeKeys).filter(c -> !c.isEmpty()).ifPresent(removeFunc::apply);
            Optional.ofNullable(addKeys).filter(c -> !c.isEmpty()).ifPresent(keys -> {
                keys = keys.stream().filter(StringUtils::hasText).collect(Collectors.toList());
                addFunc.apply(keys);
            });

        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }

    /**
     * 处理关系Id列表差异 并执行处理函数 <br>
     * 推荐使用 如果需要移除的keys为空 则不会执行移除函数 <br>
     *
     * @param oldKeys     旧的关系Id列表
     * @param newKeys     新的关系Id列表
     * @param removeFunc  处理需要移除的keys函数 (removeKeys)=>{ ... }
     * @param addFunc     处理需要增加的keys函数 (addKeys)=>{ ... }
     * @param updatedFunc 有任何修改时调用的函数 ()=>{ ... }
     */
    public static void handle(Collection<String> oldKeys, Collection<String> newKeys, HandleFunction removeFunc, HandleFunction addFunc, EmptyFunction updatedFunc) {
        Map<String, List<String>> result = new HashMap<>();
        try {
            List<String> removeKeys = ListDiffUtil.subList(oldKeys, newKeys);
            List<String> addKeys = ListDiffUtil.subList(newKeys, oldKeys);

            Optional.ofNullable(removeKeys).filter(c -> !c.isEmpty()).ifPresent(removeFunc::apply);
            Optional.ofNullable(addKeys).filter(c -> !c.isEmpty()).ifPresent(keys -> {
                keys = keys.stream().filter(StringUtils::hasText).collect(Collectors.toList());
                addFunc.apply(keys);
            });

            if (!CollectionUtils.isEmpty(removeKeys) || !CollectionUtils.isEmpty(addKeys)) {
                updatedFunc.apply();
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }

    @FunctionalInterface
    public interface FullHandleFunction {

        void apply(List<String> removeKeys, List<String> addKeys);

    }

    @FunctionalInterface
    public interface HandleFunction {

        void apply(List<String> keys);

    }

    @FunctionalInterface
    public interface EmptyFunction {

        void apply();

    }

}
