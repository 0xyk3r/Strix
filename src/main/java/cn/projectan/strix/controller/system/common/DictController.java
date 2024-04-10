package cn.projectan.strix.controller.system.common;

import cn.projectan.strix.controller.system.base.BaseSystemController;
import cn.projectan.strix.core.ret.RetBuilder;
import cn.projectan.strix.core.ret.RetResult;
import cn.projectan.strix.model.response.common.CommonDictResp;
import cn.projectan.strix.model.response.common.CommonDictVersionResp;
import cn.projectan.strix.service.DictService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author ProjectAn
 * @date 2023/5/30 10:43
 */
@Slf4j
@RestController("SystemCommonDictController")
@RequestMapping("system/common/dict")
@RequiredArgsConstructor
public class DictController extends BaseSystemController {

    private final DictService dictService;

    @GetMapping("_version")
    public RetResult<CommonDictVersionResp> getVersionList() {
        return RetBuilder.success(dictService.getDictVersionMapResp());
    }

    @GetMapping("{dictKey}")
    public RetResult<CommonDictResp> getDictData(@PathVariable String dictKey) {
        Assert.hasText(dictKey, "参数错误");
        CommonDictResp commonDictResp = dictService.getDictResp(dictKey);
        Assert.notNull(commonDictResp, "字典未找到");

        return RetBuilder.success(commonDictResp);
    }

}
