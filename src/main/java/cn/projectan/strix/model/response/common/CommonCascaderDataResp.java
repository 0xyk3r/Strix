package cn.projectan.strix.model.response.common;

import cn.hutool.core.util.StrUtil;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
 * @author 安炯奕
 * @date 2021/7/19 11:14
 */
@Data
@Slf4j
@NoArgsConstructor
public class CommonCascaderDataResp {

    private List<CascaderDataItem> options = new ArrayList<>();

    public <T> CommonCascaderDataResp(List<T> data) {
        this(data, "id", "name", "parentId", "0");
    }

    /**
     * 构造级联选择框数据
     * 注意：如果指定了value, label, status字段名称，则需要确保其具有getter方法，且获取到的值不为null，如果为null则会导致该条数据被忽略
     *
     * @param data              需要处理的集合，可传入任意实体类型
     * @param valueFieldName    value字段名称
     * @param labelFieldName    label字段名称
     * @param relationFieldName relation字段名称
     * @param <T>               任意实体类型
     */
    public <T> CommonCascaderDataResp(List<T> data, String valueFieldName, String labelFieldName, String relationFieldName, String rootRelation) {
        try {
            for (T d : data) {
                Class<?> clazz = d.getClass();
                Method keyGetter = clazz.getMethod("get" + StrUtil.upperFirst(valueFieldName));
                Object keyGetterInvoke = keyGetter.invoke(d);
                Method labelGetter = clazz.getMethod("get" + StrUtil.upperFirst(labelFieldName));
                Object labelGetterInvoke = labelGetter.invoke(d);
                Method relationGetter = clazz.getMethod("get" + StrUtil.upperFirst(relationFieldName));
                Object relationGetterInvoke = relationGetter.invoke(d);
                if (rootRelation.equals(relationGetterInvoke) && keyGetterInvoke != null && labelGetterInvoke != null) {
                    options.add(new CascaderDataItem(keyGetterInvoke.toString(), labelGetterInvoke.toString(), findChildren(data, valueFieldName, labelFieldName, relationFieldName, keyGetterInvoke.toString())));
                }
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }

    public <T> List<CascaderDataItem> findChildren(List<T> data, String valueFieldName, String labelFieldName, String relationFieldName, String parentValue) {
        List<CascaderDataItem> result = new ArrayList<>();
        try {
            for (T d : data) {
                Class<?> clazz = d.getClass();
                Method keyGetter = clazz.getMethod("get" + StrUtil.upperFirst(valueFieldName));
                Object keyGetterInvoke = keyGetter.invoke(d);
                Method labelGetter = clazz.getMethod("get" + StrUtil.upperFirst(labelFieldName));
                Object labelGetterInvoke = labelGetter.invoke(d);
                Method relationGetter = clazz.getMethod("get" + StrUtil.upperFirst(relationFieldName));
                Object relationGetterInvoke = relationGetter.invoke(d);
                if (parentValue.equals(relationGetterInvoke.toString()) && keyGetterInvoke != null && labelGetterInvoke != null) {
                    result.add(new CascaderDataItem(keyGetterInvoke.toString(), labelGetterInvoke.toString(), findChildren(data, valueFieldName, labelFieldName, relationFieldName, keyGetterInvoke.toString())));
                }
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
        return result.size() > 0 ? result : null;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CascaderDataItem {

        private String value;

        private String label;

        private List<CascaderDataItem> children;

    }
}
