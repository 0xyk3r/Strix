package cn.projectan.strix.model.response.common;

import cn.projectan.strix.utils.ReflectUtil;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * @author 安炯奕
 * @date 2021/7/19 11:14
 */
@Data
@Slf4j
@NoArgsConstructor
public class CommonSelectDataResp {

    private List<SelectDataItem> options = new ArrayList<>();

    public <T> CommonSelectDataResp(List<T> data) {
        this(data, "id", "name", null);
    }

    /**
     * 构造下拉框数据
     * 注意：如果指定了value, label, status字段名称，则需要确保其具有getter方法，且获取到的值不为null，如果为null则会导致该条数据被忽略
     *
     * @param data            需要处理的集合，可传入任意实体类型
     * @param valueFieldName  value字段名称
     * @param labelFieldName  label字段名称
     * @param attachFieldName 附加字段名称
     * @param <T>             任意实体类型
     */
    public <T> CommonSelectDataResp(List<T> data, String valueFieldName, String labelFieldName, String attachFieldName) {
        try {
            for (T d : data) {
                String value = ReflectUtil.getString(d, valueFieldName);
                String label = ReflectUtil.getString(d, labelFieldName);
                String attach = StringUtils.hasText(attachFieldName) ? ReflectUtil.getString(d, attachFieldName) : null;

                if (value != null && label != null) {
                    options.add(new SelectDataItem(value, label, attach));
                }
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SelectDataItem {

        private String value;

        private String label;

        private String attach;

    }
}
