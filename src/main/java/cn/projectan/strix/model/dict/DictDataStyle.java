package cn.projectan.strix.model.dict;

import cn.projectan.strix.model.annotation.Dict;
import cn.projectan.strix.model.annotation.DictData;

/**
 * @author ProjectAn
 * @date 2023/6/9 17:12
 */
@Dict(key = "DictDataStyle", value = "系统字典-数据-样式")
public interface DictDataStyle {

    @DictData(label = "默认", sort = 1, style = DictDataStyle.DEFAULT)
    String DEFAULT = "default";

    @DictData(label = "主要", sort = 2, style = DictDataStyle.PRIMARY)
    String PRIMARY = "primary";

    @DictData(label = "成功", sort = 3, style = DictDataStyle.SUCCESS)
    String SUCCESS = "success";

    @DictData(label = "警告", sort = 4, style = DictDataStyle.WARNING)
    String WARNING = "warning";

    @DictData(label = "危险", sort = 5, style = DictDataStyle.ERROR)
    String ERROR = "error";

    @DictData(label = "信息", sort = 6, style = DictDataStyle.INFO)
    String INFO = "info";

    static boolean valid(String value) {
        return value.equals(DEFAULT) || value.equals(PRIMARY) || value.equals(SUCCESS) || value.equals(WARNING) || value.equals(ERROR) || value.equals(INFO);
    }

}
