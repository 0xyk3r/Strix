package cn.projectan.strix.controller.system;

import cn.projectan.strix.controller.system.base.BaseSystemController;
import cn.projectan.strix.core.ret.RetBuilder;
import cn.projectan.strix.core.ret.RetResult;
import cn.projectan.strix.core.validation.group.InsertGroup;
import cn.projectan.strix.core.validation.group.UpdateGroup;
import cn.projectan.strix.model.annotation.StrixLog;
import cn.projectan.strix.model.db.system.SystemConfig;
import cn.projectan.strix.model.dict.system.SystemLogOperType;
import cn.projectan.strix.model.event.cache.ConfigChangedEvent;
import cn.projectan.strix.model.request.system.config.SystemConfigUpdateReq;
import cn.projectan.strix.model.response.system.config.SystemConfigListResp;
import cn.projectan.strix.model.response.system.config.SystemConfigResp;
import cn.projectan.strix.service.system.SystemConfigService;
import cn.projectan.strix.util.common.I18nUtil;
import cn.projectan.strix.util.common.UpdateBuilder;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 系统配置管理
 *
 * @author ProjectAn
 * @since 2026-04-16
 */
@Slf4j
@RestController
@RequestMapping("system/config")
@RequiredArgsConstructor
@Tag(name = "系统 - 配置管理")
public class SystemConfigController extends BaseSystemController {

    private final SystemConfigService systemConfigService;
    private final ApplicationEventPublisher eventPublisher;

    @Operation(summary = "配置列表")
    @GetMapping("")
    @PreAuthorize("@ss.hasPermission('system:config')")
    @StrixLog(operationGroup = "系统配置", operationName = "查询配置列表")
    public RetResult<SystemConfigListResp> list(
            @RequestParam(required = false) String keyword) {
        List<SystemConfig> list = systemConfigService.lambdaQuery()
                .like(StringUtils.hasText(keyword), SystemConfig::getName, keyword)
                .or(StringUtils.hasText(keyword))
                .like(StringUtils.hasText(keyword), SystemConfig::getKey, keyword)
                .orderByAsc(SystemConfig::getCreatedTime)
                .list();
        return RetBuilder.success(new SystemConfigListResp(list));
    }

    @Operation(summary = "配置详情")
    @GetMapping("{id}")
    @PreAuthorize("@ss.hasPermission('system:config')")
    public RetResult<SystemConfigResp> detail(@Parameter(description = "配置 ID") @PathVariable String id) {
        SystemConfig config = systemConfigService.getById(id);
        Assert.notNull(config, I18nUtil.notFound("field.config"));
        return RetBuilder.success(new SystemConfigResp(config));
    }

    @Operation(summary = "新增配置")
    @PostMapping("add")
    @PreAuthorize("@ss.hasPermission('system:config:add')")
    @StrixLog(operationGroup = "系统配置", operationName = "新增配置", operationType = SystemLogOperType.ADD)
    public RetResult<Void> add(@RequestBody @Validated(InsertGroup.class) SystemConfigUpdateReq req) {
        // 检查 key 唯一性
        SystemConfig existing = systemConfigService.getByKey(req.getKey());
        Assert.isNull(existing, "配置项标识已存在: " + req.getKey());

        SystemConfig config = new SystemConfig()
                .setKey(req.getKey())
                .setName(req.getName())
                .setType(req.getType())
                .setValue(req.getValue())
                .setRemark(req.getRemark());

        Assert.isTrue(systemConfigService.save(config), "保存配置失败");
        eventPublisher.publishEvent(new ConfigChangedEvent(this, config.getKey()));
        return RetBuilder.success();
    }

    @Operation(summary = "修改配置")
    @PostMapping("update/{id}")
    @PreAuthorize("@ss.hasPermission('system:config:update')")
    @StrixLog(operationGroup = "系统配置", operationName = "修改配置", operationType = SystemLogOperType.UPDATE)
    public RetResult<Void> update(
            @Parameter(description = "配置 ID") @PathVariable String id,
            @RequestBody @Validated(UpdateGroup.class) SystemConfigUpdateReq req) {
        SystemConfig existing = systemConfigService.getById(id);
        Assert.notNull(existing, I18nUtil.notFound("field.config"));

        String oldKey = existing.getKey();

        LambdaUpdateWrapper<SystemConfig> updateWrapper = UpdateBuilder.build(existing, req);
        Assert.isTrue(systemConfigService.update(updateWrapper), "保存配置失败");

        // 清除旧 key 和新 key 的缓存
        eventPublisher.publishEvent(new ConfigChangedEvent(this, oldKey));
        if (!oldKey.equals(existing.getKey())) {
            eventPublisher.publishEvent(new ConfigChangedEvent(this, existing.getKey()));
        }

        return RetBuilder.success();
    }

    @Operation(summary = "删除配置")
    @PostMapping("remove/{id}")
    @PreAuthorize("@ss.hasPermission('system:config:remove')")
    @StrixLog(operationGroup = "系统配置", operationName = "删除配置", operationType = SystemLogOperType.DELETE)
    public RetResult<Void> remove(@Parameter(description = "配置 ID") @PathVariable String id) {
        SystemConfig existing = systemConfigService.getById(id);
        Assert.notNull(existing, I18nUtil.notFound("field.config"));

        Assert.isTrue(systemConfigService.removeById(id), "删除配置失败");
        eventPublisher.publishEvent(new ConfigChangedEvent(this, existing.getKey()));
        return RetBuilder.success();
    }
}
