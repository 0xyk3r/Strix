package cn.projectan.strix.controller.system.tool;

import cn.projectan.strix.controller.system.base.BaseSystemController;
import cn.projectan.strix.core.ret.RetMarker;
import cn.projectan.strix.core.ret.RetResult;
import cn.projectan.strix.core.validation.group.InsertGroup;
import cn.projectan.strix.core.validation.group.UpdateGroup;
import cn.projectan.strix.model.annotation.StrixLog;
import cn.projectan.strix.model.db.PopularityConfig;
import cn.projectan.strix.model.db.PopularityData;
import cn.projectan.strix.model.dict.SysLogOperType;
import cn.projectan.strix.model.request.base.BasePageReq;
import cn.projectan.strix.model.request.system.tool.popularity.PopularityConfigUpdateReq;
import cn.projectan.strix.model.request.system.tool.popularity.PopularityDataUpdateReq;
import cn.projectan.strix.model.response.system.tool.popularity.PopularityConfigListResp;
import cn.projectan.strix.model.response.system.tool.popularity.PopularityConfigResp;
import cn.projectan.strix.model.response.system.tool.popularity.PopularityDataListResp;
import cn.projectan.strix.service.PopularityConfigService;
import cn.projectan.strix.service.PopularityDataService;
import cn.projectan.strix.utils.UniqueDetectionTool;
import cn.projectan.strix.utils.UpdateConditionBuilder;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.util.Assert;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * @author 安炯奕
 * @date 2023/10/5 21:24
 */
@Slf4j
@RestController
@RequestMapping("system/tool/popularity")
@RequiredArgsConstructor
public class PopularityController extends BaseSystemController {

    private final PopularityConfigService popularityConfigService;
    private final PopularityDataService popularityDataService;

    @GetMapping("")
    @PreAuthorize("@ss.hasPermission('system:tool:popularity')")
    @StrixLog(operationGroup = "系统工具-热度工具", operationName = "查询配置列表")
    public RetResult<PopularityConfigListResp> list() {
        List<PopularityConfig> list = popularityConfigService.lambdaQuery()
                .select(PopularityConfig::getId, PopularityConfig::getName)
                .list();
        return RetMarker.makeSuccessRsp(
                new PopularityConfigListResp(list)
        );
    }

    @GetMapping("{id}")
    @PreAuthorize("@ss.hasPermission('system:tool:popularity')")
    @StrixLog(operationGroup = "系统工具-热度工具", operationName = "查询配置信息")
    public RetResult<PopularityConfigResp> info(@PathVariable String id) {
        PopularityConfig data = popularityConfigService.getById(id);
        Assert.notNull(data, "数据不存在");

        return RetMarker.makeSuccessRsp(
                new PopularityConfigResp(data)
        );
    }

    @PostMapping("update")
    @PreAuthorize("@ss.hasPermission('system:tool:popularity:add')")
    @StrixLog(operationGroup = "系统工具-热度工具", operationName = "新增配置", operationType = SysLogOperType.ADD)
    public RetResult<Object> update(@RequestBody @Validated(InsertGroup.class) PopularityConfigUpdateReq req) {
        Assert.notNull(req, "参数错误");

        PopularityConfig popularityConfig = new PopularityConfig(
                req.getName(),
                req.getConfigKey(),
                req.getInitialValue(),
                req.getExtraValue(),
                req.getMagValue().doubleValue()
        );
        UniqueDetectionTool.check(popularityConfig);

        Assert.isTrue(popularityConfigService.save(popularityConfig), "保存失败");

        return RetMarker.makeSuccessRsp();
    }

    @PostMapping("update/{id}")
    @PreAuthorize("@ss.hasPermission('system:tool:popularity:update')")
    @StrixLog(operationGroup = "系统工具-热度工具", operationName = "修改配置", operationType = SysLogOperType.UPDATE)
    public RetResult<Object> update(@PathVariable String id, @RequestBody @Validated(UpdateGroup.class) PopularityConfigUpdateReq req) {
        PopularityConfig data = popularityConfigService.getById(id);
        Assert.notNull(data, "数据不存在");

        UpdateWrapper<PopularityConfig> updateWrapper = UpdateConditionBuilder.build(data, req);
        UniqueDetectionTool.check(data);
        Assert.isTrue(popularityConfigService.update(updateWrapper), "保存失败");

        return RetMarker.makeSuccessRsp();
    }

    @PostMapping("remove/{id}")
    @PreAuthorize("@ss.hasPermission('system:tool:popularity:remove')")
    @StrixLog(operationGroup = "系统工具-热度工具", operationName = "删除配置", operationType = SysLogOperType.DELETE)
    public RetResult<Object> remove(@PathVariable String id) {
        PopularityConfig data = popularityConfigService.getById(id);
        Assert.notNull(data, "数据不存在");

        popularityConfigService.removeById(id);
        // 删除对应数据
        popularityDataService.lambdaUpdate()
                .eq(PopularityData::getConfigKey, data.getConfigKey())
                .remove();
        // 删除缓存数据
        popularityConfigService.clearCache(data.getConfigKey());

        return RetMarker.makeSuccessRsp();
    }

    @GetMapping("{id}/data")
    @PreAuthorize("@ss.hasPermission('system:tool:popularity:data')")
    @StrixLog(operationGroup = "系统工具-热度工具", operationName = "查询数据列表")
    public RetResult<PopularityDataListResp> dataList(@PathVariable String id, BasePageReq<PopularityData> req) {
        PopularityConfig config = popularityConfigService.getById(id);
        Assert.notNull(config, "数据不存在");

        Page<PopularityData> list = popularityDataService.lambdaQuery()
                .eq(PopularityData::getConfigKey, config.getConfigKey())
                .page(req.getPage());
        return RetMarker.makeSuccessRsp(
                new PopularityDataListResp(list)
        );
    }

    @PostMapping("{id}/data/update/{dataId}")
    @PreAuthorize("@ss.hasPermission('system:tool:popularity:data')")
    @StrixLog(operationGroup = "系统工具-热度工具", operationName = "修改热度数据", operationType = SysLogOperType.UPDATE)
    public RetResult<Object> updateData(@PathVariable String id, @PathVariable String dataId, @RequestBody @Validated(UpdateGroup.class) PopularityDataUpdateReq req) {
        PopularityConfig config = popularityConfigService.getById(id);
        Assert.notNull(config, "数据不存在");

        popularityDataService.lambdaUpdate()
                .eq(PopularityData::getConfigKey, config.getConfigKey())
                .eq(PopularityData::getId, dataId)
                .set(PopularityData::getOriginalValue, req.getOriginalValue())
                .update();

        return RetMarker.makeSuccessRsp();
    }

    @PostMapping("{id}/data/remove/{dataId}")
    @PreAuthorize("@ss.hasPermission('system:tool:popularity:data')")
    @StrixLog(operationGroup = "系统工具-热度工具", operationName = "删除热度数据", operationType = SysLogOperType.DELETE)
    public RetResult<Object> removeData(@PathVariable String id, @PathVariable String dataId) {
        PopularityConfig config = popularityConfigService.getById(id);
        Assert.notNull(config, "数据不存在");

        popularityDataService.lambdaUpdate()
                .eq(PopularityData::getConfigKey, config.getConfigKey())
                .eq(PopularityData::getId, dataId)
                .remove();

        return RetMarker.makeSuccessRsp();
    }

}
