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
 * 通用穿梭框数据
 *
 * @author ProjectAn
 * @date 2021/7/6 15:58
 */
@Data
@Slf4j
public class CommonTransferDataResp {

    private List<TransferDataItem> transferData = new ArrayList<>();

    public <T> CommonTransferDataResp(List<T> data) {
        this(data, "id", "name", null);
    }

    /**
     * 构造穿梭框数据
     * 注意：如果指定了value, label, status字段名称，则需要确保其具有getter方法，且获取到的值不为null，如果为null则会导致该条数据被忽略
     *
     * @param data            需要处理的集合，可传入任意实体类型
     * @param valueFieldName  value字段名称
     * @param labelFieldName  label字段名称
     * @param statusFieldName status字段名称
     * @param <T>             任意实体类型
     */
    public <T> CommonTransferDataResp(List<T> data, String valueFieldName, String labelFieldName, String statusFieldName) {
        try {
            for (T d : data) {
                String value = ReflectUtil.getString(d, valueFieldName);
                String label = ReflectUtil.getString(d, labelFieldName);
                Integer status = StringUtils.hasText(statusFieldName) ? ReflectUtil.getInteger(d, statusFieldName) : null;

                if (value != null && label != null) {
                    transferData.add(new TransferDataItem(value, label, status));
                }
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    private static class TransferDataItem {

        private String value;

        private String label;

        private Integer status;

    }

}
