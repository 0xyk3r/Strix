package cn.projectan.strix.model.dict.system;

import cn.projectan.strix.model.annotation.Dict;
import cn.projectan.strix.model.annotation.DictData;
import cn.projectan.strix.model.dict.base.BaseDict;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 字典变更类型
 *
 * @author ProjectAn
 * @since 2026-04-19
 */
@Dict(key = "DictChangeType", value = "字典变更类型")
@Schema(description = "字典变更类型")
public class DictChangeType implements BaseDict {

    @DictData(label = "字典创建", sort = 1, style = DictDataStyle.SUCCESS)
    public static final String DICT_CREATED = "DICT_CREATED";

    @DictData(label = "字典修改", sort = 2, style = DictDataStyle.PRIMARY)
    public static final String DICT_UPDATED = "DICT_UPDATED";

    @DictData(label = "字典删除", sort = 3, style = DictDataStyle.ERROR)
    public static final String DICT_DELETED = "DICT_DELETED";

    @DictData(label = "数据新增", sort = 4, style = DictDataStyle.SUCCESS)
    public static final String DATA_ADDED = "DATA_ADDED";

    @DictData(label = "数据修改", sort = 5, style = DictDataStyle.PRIMARY)
    public static final String DATA_UPDATED = "DATA_UPDATED";

    @DictData(label = "数据删除", sort = 6, style = DictDataStyle.ERROR)
    public static final String DATA_DELETED = "DATA_DELETED";

    @DictData(label = "数据排序", sort = 7, style = DictDataStyle.INFO)
    public static final String DATA_SORTED = "DATA_SORTED";

    @DictData(label = "字典克隆", sort = 8, style = DictDataStyle.INFO)
    public static final String DICT_CLONED = "DICT_CLONED";

    @DictData(label = "字典导入", sort = 9, style = DictDataStyle.WARNING)
    public static final String DICT_IMPORTED = "DICT_IMPORTED";

}
