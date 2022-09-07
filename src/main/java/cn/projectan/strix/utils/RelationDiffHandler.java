package cn.projectan.strix.utils;

import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 关系表差异更变处理器
 *
 * @author 安炯奕
 * @date 2021/7/2 17:21
 */
@Slf4j
public class RelationDiffHandler {

    public static Map<String, List<String>> handle(List<String> oldKeys, List<String> newKeys) {
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

}
