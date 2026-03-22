package cn.projectan.strix.util.algo;

import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 集合差异处理器
 *
 * @author ProjectAn
 * @since 2021/7/2 17:21
 */
@Slf4j
public class KeyDiffUtil {

    /**
     * 处理关系Id列表差异 并执行处理函数
     * <p>如果需要移除的keys为空 则不会执行移除函数
     *
     * @param oldKeys    旧的关系Id列表
     * @param newKeys    新的关系Id列表
     * @param removeFunc 处理需要移除的keys函数 (removeKeys)=>{ ... }
     * @param addFunc    处理需要增加的keys函数 (addKeys)=>{ ... }
     */
    public static void handle(Collection<String> oldKeys, Collection<String> newKeys, HandleFunction removeFunc, HandleFunction addFunc) {
        try {
            List<String> removeKeys = CollectionDiffUtil.subList(oldKeys, newKeys);
            List<String> addKeys = CollectionDiffUtil.subList(newKeys, oldKeys);

            removeKeys = removeKeys.stream().filter(StringUtils::hasText).collect(Collectors.toList());
            addKeys = addKeys.stream().filter(StringUtils::hasText).collect(Collectors.toList());
            if (!removeKeys.isEmpty()) {
                removeFunc.apply(removeKeys);
            }
            if (!addKeys.isEmpty()) {
                addFunc.apply(addKeys);
            }
        } catch (Exception e) {
            log.error("差异处理异常: {}", e.getMessage(), e);
        }
    }

    /**
     * 处理关系Id列表差异 并执行处理函数
     * <p>如果需要移除的keys为空 则不会执行移除函数
     *
     * @param oldKeys     旧的关系Id列表
     * @param newKeys     新的关系Id列表
     * @param removeFunc  处理需要移除的keys函数 (removeKeys)=>{ ... }
     * @param addFunc     处理需要增加的keys函数 (addKeys)=>{ ... }
     * @param updatedFunc 有任何修改时调用的函数 ()=>{ ... }
     */
    public static void handle(Collection<String> oldKeys, Collection<String> newKeys, HandleFunction removeFunc, HandleFunction addFunc, EmptyFunction updatedFunc) {
        try {
            List<String> removeKeys = CollectionDiffUtil.subList(oldKeys, newKeys);
            List<String> addKeys = CollectionDiffUtil.subList(newKeys, oldKeys);

            removeKeys = removeKeys.stream().filter(StringUtils::hasText).collect(Collectors.toList());
            addKeys = addKeys.stream().filter(StringUtils::hasText).collect(Collectors.toList());
            if (!removeKeys.isEmpty()) {
                removeFunc.apply(removeKeys);
            }
            if (!addKeys.isEmpty()) {
                addFunc.apply(addKeys);
            }

            if (!removeKeys.isEmpty() || !addKeys.isEmpty()) {
                updatedFunc.apply();
            }
        } catch (Exception e) {
            log.error("差异处理异常: {}", e.getMessage(), e);
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
