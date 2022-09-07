package cn.projectan.strix.model.response.common;

import cn.hutool.core.util.StrUtil;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
 * 通用穿梭框数据
 *
 * @author 安炯奕
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
     * 注意：如果指定了key, label, status字段名称，则需要确保其具有getter方法，且获取到的值不为null，如果为null则会导致该条数据被忽略
     *
     * @param data            需要处理的集合，可传入任意实体类型
     * @param keyFieldName    key字段名称
     * @param labelFieldName  label字段名称
     * @param statusFieldName status字段名称
     * @param <T>             任意实体类型
     */
    public <T> CommonTransferDataResp(List<T> data, String keyFieldName, String labelFieldName, String statusFieldName) {
        try {
            for (T d : data) {
                Class<?> clazz = d.getClass();
                Method keyGetter = clazz.getMethod("get" + StrUtil.upperFirst(keyFieldName));
                Object keyGetterInvoke = keyGetter.invoke(d);
                Method labelGetter = clazz.getMethod("get" + StrUtil.upperFirst(labelFieldName));
                Object labelGetterInvoke = labelGetter.invoke(d);
                if (StringUtils.hasText(statusFieldName)) {
                    Method statusGetter = clazz.getMethod("get" + StrUtil.upperFirst(statusFieldName));
                    Object statusGetterInvoke = statusGetter.invoke(d);
                    if (keyGetterInvoke != null && labelGetterInvoke != null && statusGetterInvoke != null) {
                        transferData.add(new TransferDataItem(keyGetterInvoke.toString(), labelGetterInvoke.toString(), Integer.valueOf(statusGetterInvoke.toString())));
                    }
                } else {
                    if (keyGetterInvoke != null && labelGetterInvoke != null) {
                        transferData.add(new TransferDataItem(keyGetterInvoke.toString(), labelGetterInvoke.toString(), null));
                    }
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

        private String key;

        private String label;

        private Integer status;

    }

}
