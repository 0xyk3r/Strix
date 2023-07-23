package cn.projectan.strix.model.response.common;

import cn.projectan.strix.utils.ReflectUtil;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

/**
 * @author 安炯奕
 * @date 2021/9/30 13:41
 */
@Data
@Slf4j
@NoArgsConstructor
public class CommonTreeDataResp {

    private List<TreeDataItem> tree = new ArrayList<>();

    public <T> CommonTreeDataResp(List<T> data) {
        this(data, "id", "name", "parentId", "0");
    }

    /**
     * 构造树状选择框数据
     * 注意：如果指定了value, label, status字段名称，则需要确保其具有getter方法，且获取到的值不为null，如果为null则会导致该条数据被忽略
     *
     * @param data              需要处理的集合，可传入任意实体类型
     * @param valueFieldName    value字段名称
     * @param labelFieldName    label字段名称
     * @param relationFieldName relation字段名称
     * @param <T>               任意实体类型
     */
    public <T> CommonTreeDataResp(List<T> data, String valueFieldName, String labelFieldName, String relationFieldName, String rootRelation) {
        try {
            for (T d : data) {
                String value = ReflectUtil.getString(d, valueFieldName);
                String label = ReflectUtil.getString(d, labelFieldName);
                String relation = ReflectUtil.getString(d, relationFieldName);

                if (rootRelation.equals(relation) && value != null && label != null) {
                    tree.add(new TreeDataItem(value, label, findChildren(data, valueFieldName, labelFieldName, relationFieldName, value)));
                }
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }

    public <T> List<TreeDataItem> findChildren(List<T> data, String valueFieldName, String labelFieldName, String relationFieldName, String parentValue) {
        List<TreeDataItem> result = new ArrayList<>();
        try {
            for (T d : data) {
                String value = ReflectUtil.getString(d, valueFieldName);
                String label = ReflectUtil.getString(d, labelFieldName);
                String relation = ReflectUtil.getString(d, relationFieldName);

                if (parentValue.equals(relation) && value != null && label != null) {
                    result.add(new TreeDataItem(value, label, findChildren(data, valueFieldName, labelFieldName, relationFieldName, value)));
                }
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
        return result;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TreeDataItem {

        private String value;

        private String label;

        private List<TreeDataItem> children;

        private Boolean isLeaf;

        public Boolean getIsLeaf() {
            return children == null || children.size() == 0;
        }

        public TreeDataItem(String value, String label, List<TreeDataItem> children) {
            this.value = value;
            this.label = label;
            this.children = children;
        }
    }

}
