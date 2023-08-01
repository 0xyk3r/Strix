package cn.projectan.strix.model.dict;

import cn.projectan.strix.model.annotation.Dict;
import cn.projectan.strix.model.annotation.DictData;

/**
 * @author 安炯奕
 * @date 2023/6/12 18:34
 */
@Dict(key = "DictDataType", value = "系统字典-数据类型")
public interface DictDataType {

    @DictData(label = "字符串", sort = 1, style = DictDataStyle.DEFAULT)
    int STRING = 1;

    @DictData(label = "整数", sort = 2, style = DictDataStyle.INFO)
    int INTEGER = 2;

    @DictData(label = "长整数", sort = 3, style = DictDataStyle.INFO)
    int LONG = 3;

    @DictData(label = "单浮点数", sort = 4, style = DictDataStyle.SUCCESS)
    int FLOAT = 4;

    @DictData(label = "双浮点数", sort = 5, style = DictDataStyle.SUCCESS)
    int DOUBLE = 5;

    @DictData(label = "布尔值", sort = 6, style = DictDataStyle.WARNING)
    int BOOLEAN = 6;

    @DictData(label = "字节", sort = 7, style = DictDataStyle.ERROR)
    int BYTE = 7;

    static boolean valid(int value) {
        return value == STRING || value == INTEGER || value == LONG || value == FLOAT || value == DOUBLE || value == BOOLEAN || value == BYTE;
    }

}
