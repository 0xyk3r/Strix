package cn.projectan.strix.utils.context;

import java.util.HashMap;
import java.util.Map;

/**
 * 上下文持有者
 *
 * @author ProjectAn
 * @date 2023/12/9 16:21
 */
public class ContextHolder {

    private static final ThreadLocal<Map<String, Object>> context = new ThreadLocal<>();

    public static void set(String key, Object value) {
        Map<String, Object> map = context.get();
        if (map == null) {
            map = new HashMap<>();
            context.set(map);
        }
        map.put(key, value);
    }

    public static Object get(String key) {
        Map<String, Object> map = context.get();
        if (map == null) {
            return null;
        }
        return map.get(key);
    }

    public static void clear() {
        context.remove();
    }

}
