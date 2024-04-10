package cn.projectan.strix.model.dict;

import cn.projectan.strix.model.annotation.Dict;
import cn.projectan.strix.model.annotation.DictData;
import cn.projectan.strix.model.dict.base.BaseDict;
import org.springframework.stereotype.Component;

/**
 * @author ProjectAn
 * @date 2023/6/17 15:14
 */
@Component
@Dict(key = "SysLogOperType", value = "系统日志-操作类型")
public class SysLogOperType implements BaseDict {

    @DictData(label = "查询", sort = 1, style = DictDataStyle.INFO)
    public static final
    String QUERY = "query";

    @DictData(label = "新增", sort = 2, style = DictDataStyle.SUCCESS)
    public static final
    String ADD = "add";

    @DictData(label = "修改", sort = 3, style = DictDataStyle.WARNING)
    public static final
    String UPDATE = "update";

    @DictData(label = "删除", sort = 4, style = DictDataStyle.ERROR)
    public static final
    String DELETE = "delete";

    @DictData(label = "导入", sort = 5, style = DictDataStyle.DEFAULT)
    public static final
    String IMPORT = "import";

    @DictData(label = "导出", sort = 6, style = DictDataStyle.DEFAULT)
    public static final
    String EXPORT = "export";

    @DictData(label = "其他", sort = 7, style = DictDataStyle.DEFAULT)
    public static final
    String OTHER = "other";

    @DictData(label = "登录", sort = 8, style = DictDataStyle.DEFAULT)
    public static final
    String LOGIN = "login";

}
