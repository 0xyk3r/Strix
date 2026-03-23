package cn.projectan.strix.controller.system.tool;

import cn.projectan.strix.controller.system.base.BaseSystemController;
import cn.projectan.strix.core.ret.RetBuilder;
import cn.projectan.strix.core.ret.RetResult;
import cn.projectan.strix.core.validation.group.InsertGroup;
import cn.projectan.strix.core.validation.group.UpdateGroup;
import cn.projectan.strix.model.annotation.StrixLog;
import cn.projectan.strix.model.db.system.PopularityConfig;
import cn.projectan.strix.model.db.system.PopularityData;
import cn.projectan.strix.model.dict.system.SystemLogOperType;
import cn.projectan.strix.model.request.base.BasePageReq;
import cn.projectan.strix.model.request.system.tool.popularity.PopularityConfigUpdateReq;
import cn.projectan.strix.model.request.system.tool.popularity.PopularityDataUpdateReq;
import cn.projectan.strix.model.response.system.tool.popularity.PopularityConfigListResp;
import cn.projectan.strix.model.response.system.tool.popularity.PopularityConfigResp;
import cn.projectan.strix.model.response.system.tool.popularity.PopularityDataListResp;
import cn.projectan.strix.service.system.PopularityConfigService;
import cn.projectan.strix.service.system.PopularityDataService;
import cn.projectan.strix.util.common.I18nUtil;
import cn.projectan.strix.util.common.UniqueChecker;
import cn.projectan.strix.util.common.UpdateBuilder;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.util.Assert;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 热度工具
 *
 * @author ProjectAn
 * @since 2023/10/5 21:24
 */
@Slf4j
@RestController
@RequestMapping("system/tool/popularity")
@RequiredArgsConstructor
@Tag(name = "系统工具 - 热度管理")
public class PopularityController extends BaseSystemController {

    private final PopularityConfigService popularityConfigService;
    private final PopularityDataService popularityDataService;

    /**
     * 查询配置列表
     */
    @GetMapping("")
    @PreAuthorize("@ss.hasPermission('system:tool:popularity')")
    @StrixLog(operationGroup = "系统工具-热度工具", operationName = "查询配置列表")
    @Operation(summary = "热度配置列表")
    public RetResult<PopularityConfigListResp> list() {
        List<PopularityConfig> list = popularityConfigService.listAll();
        return RetBuilder.success(new PopularityConfigListResp(list));
    }

    /**
     * 查询配置信息
     */
    @GetMapping("{id}")
    @PreAuthorize("@ss.hasPermission('system:tool:popularity')")
    @StrixLog(operationGroup = "系统工具-热度工具", operationName = "查询配置信息")
    @Operation(summary = "热度配置详情")
    public RetResult<PopularityConfigResp> info(@Parameter(description = "热度配置 ID") @PathVariable String id) {
        PopularityConfig data = popularityConfigService.getById(id);
        Assert.notNull(data, I18nUtil.notFound("field.data"));
        return RetBuilder.success(new PopularityConfigResp(data));
    }

    /**
     * 新增配置
     */
    @PostMapping("update")
    @PreAuthorize("@ss.hasPermission('system:tool:popularity:add')")
    @StrixLog(operationGroup = "系统工具-热度工具", operationName = "新增配置", operationType = SystemLogOperType.ADD)
    @Operation(summary = "新增热度配置")
    public RetResult<Object> update(@RequestBody @Validated(InsertGroup.class) PopularityConfigUpdateReq req) {
        Assert.notNull(req, I18nUtil.get("error.param.invalid"));

        PopularityConfig popularityConfig = new PopularityConfig(
                req.getName(),
                req.getConfigKey(),
                req.getInitialValue(),
                req.getExtraValue(),
                req.getMagValue()
        );
        UniqueChecker.check(popularityConfig);

        Assert.isTrue(popularityConfigService.save(popularityConfig), "保存失败");
        return RetBuilder.success();
    }

    /**
     * 修改配置
     */
    @PostMapping("update/{id}")
    @PreAuthorize("@ss.hasPermission('system:tool:popularity:update')")
    @StrixLog(operationGroup = "系统工具-热度工具", operationName = "修改配置", operationType = SystemLogOperType.UPDATE)
    @Operation(summary = "编辑热度配置")
    public RetResult<Object> update(@Parameter(description = "热度配置 ID") @PathVariable String id, @RequestBody @Validated(UpdateGroup.class) PopularityConfigUpdateReq req) {
        PopularityConfig data = popularityConfigService.getById(id);
        Assert.notNull(data, I18nUtil.notFound("field.data"));

        LambdaUpdateWrapper<PopularityConfig> updateWrapper = UpdateBuilder.build(data, req);
        UniqueChecker.check(data);
        Assert.isTrue(popularityConfigService.update(updateWrapper), "保存失败");

        return RetBuilder.success();
    }

    /**
     * 删除配置
     */
    @PostMapping("remove/{id}")
    @PreAuthorize("@ss.hasPermission('system:tool:popularity:remove')")
    @StrixLog(operationGroup = "系统工具-热度工具", operationName = "删除配置", operationType = SystemLogOperType.DELETE)
    @Operation(summary = "删除热度配置")
    public RetResult<Object> remove(@Parameter(description = "热度配置 ID") @PathVariable String id) {
        PopularityConfig data = popularityConfigService.getById(id);
        Assert.notNull(data, I18nUtil.notFound("field.data"));

        popularityConfigService.removeById(id);
        // 删除对应数据
        popularityDataService.deleteByConfigKey(data.getConfigKey());
        // 删除缓存数据
        popularityConfigService.clearCache(data.getConfigKey());

        return RetBuilder.success();
    }

    /**
     * 查询数据列表
     */
    @GetMapping("{id}/data")
    @PreAuthorize("@ss.hasPermission('system:tool:popularity:data')")
    @StrixLog(operationGroup = "系统工具-热度工具", operationName = "查询数据列表")
    @Operation(summary = "热度数据列表")
    public RetResult<PopularityDataListResp> dataList(@Parameter(description = "热度配置 ID") @PathVariable String id, BasePageReq<PopularityData> req) {
        PopularityConfig config = popularityConfigService.getById(id);
        Assert.notNull(config, I18nUtil.notFound("field.data"));

        Page<PopularityData> list = popularityDataService.listPage(config.getConfigKey(), req.getPage());
        return RetBuilder.success(new PopularityDataListResp(list));
    }

    /**
     * 修改热度数据
     */
    @PostMapping("{id}/data/update/{dataId}")
    @PreAuthorize("@ss.hasPermission('system:tool:popularity:data')")
    @StrixLog(operationGroup = "系统工具-热度工具", operationName = "修改热度数据", operationType = SystemLogOperType.UPDATE)
    @Operation(summary = "更新热度数据")
    public RetResult<Object> updateData(@Parameter(description = "热度配置 ID") @PathVariable String id, @Parameter(description = "热度数据 ID") @PathVariable String dataId, @RequestBody @Validated(UpdateGroup.class) PopularityDataUpdateReq req) {
        PopularityConfig config = popularityConfigService.getById(id);
        Assert.notNull(config, I18nUtil.notFound("field.data"));

        popularityDataService.updateOriginalValue(config.getConfigKey(), dataId, req.getOriginalValue());
        return RetBuilder.success();
    }

    /**
     * 删除热度数据
     */
    @PostMapping("{id}/data/remove/{dataId}")
    @PreAuthorize("@ss.hasPermission('system:tool:popularity:data')")
    @StrixLog(operationGroup = "系统工具-热度工具", operationName = "删除热度数据", operationType = SystemLogOperType.DELETE)
    @Operation(summary = "删除热度数据")
    public RetResult<Object> removeData(@Parameter(description = "热度配置 ID") @PathVariable String id, @Parameter(description = "热度数据 ID") @PathVariable String dataId) {
        PopularityConfig config = popularityConfigService.getById(id);
        Assert.notNull(config, I18nUtil.notFound("field.data"));

        popularityDataService.deleteByConfigKeyAndId(config.getConfigKey(), dataId);
        return RetBuilder.success();
    }

}
