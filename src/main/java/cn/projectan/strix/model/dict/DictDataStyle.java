package cn.projectan.strix.model.dict;

import cn.projectan.strix.model.annotation.Dict;
import cn.projectan.strix.model.annotation.DictData;
import cn.projectan.strix.model.dict.base.BaseDict;
import org.springframework.stereotype.Component;

/**
 * @author ProjectAn
 * @date 2023/6/9 17:12
 */
@Component
@Dict(key = "DictDataStyle", value = "系统字典-数据-样式")
public class DictDataStyle implements BaseDict {

    @DictData(label = "默认", sort = 1, style = DictDataStyle.DEFAULT)
    public static final
    String DEFAULT = "default";

    @DictData(label = "主要", sort = 2, style = DictDataStyle.PRIMARY)
    public static final
    String PRIMARY = "primary";

    @DictData(label = "成功", sort = 3, style = DictDataStyle.SUCCESS)
    public static final
    String SUCCESS = "success";

    @DictData(label = "警告", sort = 4, style = DictDataStyle.WARNING)
    public static final
    String WARNING = "warning";

    @DictData(label = "危险", sort = 5, style = DictDataStyle.ERROR)
    public static final
    String ERROR = "error";

    @DictData(label = "信息", sort = 6, style = DictDataStyle.INFO)
    public static final
    String INFO = "info";

    public static boolean valid(String value) {
        return value.equals(DEFAULT) || value.equals(PRIMARY) || value.equals(SUCCESS) || value.equals(WARNING) || value.equals(ERROR) || value.equals(INFO);
    }

}
