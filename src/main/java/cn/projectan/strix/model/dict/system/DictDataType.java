package cn.projectan.strix.model.dict.system;

import cn.projectan.strix.model.annotation.Dict;
import cn.projectan.strix.model.annotation.DictData;
import cn.projectan.strix.model.dict.base.BaseDict;

/**
 * @author ProjectAn
 * @since 2023/6/12 18:34
 */
@Dict(key = "DictDataType", value = "系统字典-数据类型")
public class DictDataType implements BaseDict {

    @DictData(label = "字符串", sort = 1, style = DictDataStyle.DEFAULT)
    public static final
    short STRING = 1;

    @DictData(label = "整数", sort = 2, style = DictDataStyle.INFO)
    public static final
    short INTEGER = 2;

    @DictData(label = "长整数", sort = 3, style = DictDataStyle.INFO)
    public static final
    short LONG = 3;

    @DictData(label = "单浮点数", sort = 4, style = DictDataStyle.SUCCESS)
    public static final
    short FLOAT = 4;

    @DictData(label = "双浮点数", sort = 5, style = DictDataStyle.SUCCESS)
    public static final
    short DOUBLE = 5;

    @DictData(label = "布尔值", sort = 6, style = DictDataStyle.WARNING)
    public static final
    short BOOLEAN = 6;

    @DictData(label = "字节", sort = 7, style = DictDataStyle.ERROR)
    public static final
    short BYTE = 7;

    @DictData(label = "短整数", sort = 8, style = DictDataStyle.INFO)
    public static final
    short SHORT = 8;

    public static boolean valid(short value) {
        return value == STRING || value == INTEGER || value == LONG || value == FLOAT || value == DOUBLE || value == BOOLEAN || value == BYTE || value == SHORT;
    }

}
