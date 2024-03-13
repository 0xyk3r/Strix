package cn.projectan.strix.model.response.common;

import cn.projectan.strix.utils.ReflectUtil;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

/**
 * @author ProjectAn
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
                String value = ReflectUtil.getString(d, valueFieldName);
                String label = ReflectUtil.getString(d, labelFieldName);
                String relation = ReflectUtil.getString(d, relationFieldName);

                if (rootRelation.equals(relation) && value != null && label != null) {
                    options.add(new CascaderDataItem(value, label, findChildren(data, valueFieldName, labelFieldName, relationFieldName, value)));
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
                String value = ReflectUtil.getString(d, valueFieldName);
                String label = ReflectUtil.getString(d, labelFieldName);
                String relation = ReflectUtil.getString(d, relationFieldName);

                if (parentValue.equals(relation) && value != null && label != null) {
                    result.add(new CascaderDataItem(value, label, findChildren(data, valueFieldName, labelFieldName, relationFieldName, value)));
                }
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
        return !result.isEmpty() ? result : null;
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
