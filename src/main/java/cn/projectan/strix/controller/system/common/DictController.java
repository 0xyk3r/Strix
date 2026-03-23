package cn.projectan.strix.controller.system.common;

import cn.projectan.strix.controller.system.base.BaseSystemController;
import cn.projectan.strix.core.ret.RetBuilder;
import cn.projectan.strix.core.ret.RetResult;
import cn.projectan.strix.model.response.common.CommonDictResp;
import cn.projectan.strix.model.response.common.CommonDictVersionResp;
import cn.projectan.strix.service.system.DictService;
import cn.projectan.strix.util.common.I18nUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 字典数据
 *
 * @author ProjectAn
 * @since 2023/5/30 10:43
 */
@Slf4j
@RestController("SystemCommonDictController")
@RequestMapping("system/common/dict")
@RequiredArgsConstructor
@Tag(name = "通用 - 字典")
public class DictController extends BaseSystemController {

    private final DictService dictService;

    /**
     * 获取字典版本列表
     */
    @GetMapping("_version")
    @Operation(summary = "获取字典版本列表")
    public RetResult<CommonDictVersionResp> getVersionList() {
        return RetBuilder.success(dictService.getDictVersionMapResp());
    }

    /**
     * 获取字典数据
     */
    @GetMapping("{dictKey}")
    @Operation(summary = "获取字典数据")
    @Parameter(name = "dictKey", description = "字典 Key", required = true)
    public RetResult<CommonDictResp> getDictData(@PathVariable String dictKey) {
        CommonDictResp commonDictResp = dictService.getDictResp(dictKey);
        Assert.notNull(commonDictResp, I18nUtil.notFound("field.dict"));

        return RetBuilder.success(commonDictResp);
    }

}
