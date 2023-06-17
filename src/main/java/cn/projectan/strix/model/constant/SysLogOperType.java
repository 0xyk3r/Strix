package cn.projectan.strix.model.constant;

import cn.projectan.strix.model.annotation.Dict;
import cn.projectan.strix.model.annotation.DictData;

/**
 * @author 安炯奕
 * @date 2023/6/17 15:14
 */
@Dict(key = "SysLogOperType", value = "系统日志-操作类型")
public interface SysLogOperType {

    @DictData(label = "查询", sort = 1, style = DictDataStyle.INFO)
    String QUERY = "query";

    @DictData(label = "新增", sort = 2, style = DictDataStyle.SUCCESS)
    String ADD = "add";

    @DictData(label = "修改", sort = 3, style = DictDataStyle.WARNING)
    String UPDATE = "update";

    @DictData(label = "删除", sort = 4, style = DictDataStyle.ERROR)
    String DELETE = "delete";

    @DictData(label = "导入", sort = 5, style = DictDataStyle.DEFALUT)
    String IMPORT = "import";

    @DictData(label = "导出", sort = 6, style = DictDataStyle.DEFALUT)
    String EXPORT = "export";

    @DictData(label = "其他", sort = 7, style = DictDataStyle.DEFALUT)
    String OTHER = "other";

    @DictData(label = "登录", sort = 8, style = DictDataStyle.DEFALUT)
    String LOGIN = "login";

}
