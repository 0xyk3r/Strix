package cn.projectan.strix.controller.system.module.ai;

import cn.projectan.strix.controller.system.base.BaseSystemController;
import cn.projectan.strix.core.module.ai.AiModelStore;
import cn.projectan.strix.core.ret.RetBuilder;
import cn.projectan.strix.core.ret.RetResult;
import cn.projectan.strix.model.annotation.StrixLog;
import cn.projectan.strix.model.db.system.AiModelConfig;
import cn.projectan.strix.model.dict.system.SystemLogOperType;
import cn.projectan.strix.model.request.system.module.ai.AiModelConfigUpdateReq;
import cn.projectan.strix.model.response.system.ai.AiModelConfigResp;
import cn.projectan.strix.service.system.AiModelConfigService;
import cn.projectan.strix.util.common.I18nUtil;
import cn.projectan.strix.util.common.UniqueChecker;
import cn.projectan.strix.util.common.UpdateBuilder;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.Assert;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * AI 模型配置管理
 *
 * @author ProjectAn
 * @since 2026-05-12
 */
@Slf4j
@RestController
@RequestMapping("system/ai/model-config")
@RequiredArgsConstructor
@Tag(name = "系统模块 - AI 模型配置")
public class AiModelConfigController extends BaseSystemController {

    private final AiModelConfigService aiModelConfigService;
    private final AiModelStore aiModelStore;

    /**
     * 查询模型配置列表
     */
    @GetMapping("")
    @StrixLog(operationGroup = "AI 模型配置", operationName = "查询配置列表")
    @Operation(summary = "AI 模型配置列表")
    public RetResult<List<AiModelConfigResp>> getList() {
        List<AiModelConfig> list = aiModelConfigService.lambdaQuery()
                .orderByAsc(AiModelConfig::getType)
                .orderByAsc(AiModelConfig::getCreatedTime)
                .list();
        return RetBuilder.success(list.stream().map(AiModelConfigResp::from).toList());
    }

    /**
     * 查询模型配置详情
     */
    @GetMapping("{id}")
    @StrixLog(operationGroup = "AI 模型配置", operationName = "查询配置详情")
    @Operation(summary = "AI 模型配置详情")
    public RetResult<AiModelConfigResp> getInfo(@Parameter(description = "配置 ID") @PathVariable String id) {
        AiModelConfig config = aiModelConfigService.getById(id);
        Assert.notNull(config, I18nUtil.notFound("field.config"));
        return RetBuilder.success(AiModelConfigResp.from(config));
    }

    /**
     * 新增模型配置
     */
    @PostMapping("update")
    @StrixLog(operationGroup = "AI 模型配置", operationName = "新增配置", operationType = SystemLogOperType.ADD)
    @Operation(summary = "新增 AI 模型配置")
    public RetResult<Void> add(@RequestBody @Validated(cn.projectan.strix.core.validation.group.InsertGroup.class) AiModelConfigUpdateReq req) {
        AiModelConfig config = new AiModelConfig()
                .setKey(req.getKey())
                .setName(req.getName())
                .setType(req.getType())
                .setBaseUrl(req.getBaseUrl())
                .setApiKey(req.getApiKey())
                .setModelName(req.getModelName())
                .setTemperature(req.getTemperature())
                .setTopP(req.getTopP())
                .setMaxTokens(req.getMaxTokens())
                .setSystemPrompt(req.getSystemPrompt())
                .setEnableThinking(req.getEnableThinking())
                .setThinkingBudget(req.getThinkingBudget())
                .setVoice(req.getVoice())
                .setSpeed(req.getSpeed())
                .setResponseFormat(req.getResponseFormat())
                .setLanguage(req.getLanguage())
                .setStatus(req.getStatus() != null ? req.getStatus() : 1)
                .setRemark(req.getRemark());

        UniqueChecker.check(config);
        Assert.isTrue(aiModelConfigService.save(config), "保存失败");
        return RetBuilder.success();
    }

    /**
     * 修改模型配置
     */
    @PostMapping("update/{id}")
    @StrixLog(operationGroup = "AI 模型配置", operationName = "修改配置", operationType = SystemLogOperType.UPDATE)
    @Operation(summary = "编辑 AI 模型配置")
    public RetResult<Void> update(@Parameter(description = "配置 ID") @PathVariable String id,
                                  @RequestBody @Validated(cn.projectan.strix.core.validation.group.UpdateGroup.class) AiModelConfigUpdateReq req) {
        AiModelConfig config = aiModelConfigService.getById(id);
        Assert.notNull(config, I18nUtil.notFound("field.originalData"));

        LambdaUpdateWrapper<AiModelConfig> wrapper = UpdateBuilder.build(config, req);
        UniqueChecker.check(config);
        Assert.isTrue(aiModelConfigService.update(wrapper), "保存失败");
        aiModelStore.invalidate(config.getKey());

        return RetBuilder.success();
    }

    /**
     * 删除模型配置
     */
    @PostMapping("remove/{id}")
    @StrixLog(operationGroup = "AI 模型配置", operationName = "删除配置", operationType = SystemLogOperType.DELETE)
    @Operation(summary = "删除 AI 模型配置")
    public RetResult<Void> remove(@Parameter(description = "配置 ID") @PathVariable String id) {
        AiModelConfig config = aiModelConfigService.getById(id);
        Assert.notNull(config, I18nUtil.notFound("field.originalData"));
        Assert.isTrue(aiModelConfigService.removeById(id), "删除失败");
        aiModelStore.invalidate(config.getKey());
        return RetBuilder.success();
    }

    /**
     * 测试模型配置连通性（仅对文本/视觉类模型有效）
     */
    @PostMapping("test/{id}")
    @StrixLog(operationGroup = "AI 模型配置", operationName = "测试配置连通性")
    @Operation(summary = "测试 AI 模型配置连通性")
    public RetResult<String> testConnection(@Parameter(description = "配置 ID") @PathVariable String id) {
        AiModelConfig config = aiModelConfigService.getById(id);
        Assert.notNull(config, I18nUtil.notFound("field.config"));
        Assert.isTrue(config.getStatus() != null && config.getStatus() == 1, "配置未启用");

        aiModelStore.invalidate(config.getKey());

        try {
            aiModelStore.getChatModel(config);
            return RetBuilder.success("配置验证通过");
        } catch (Exception e) {
            log.warn("AI 模型配置连通性测试失败: key={}, error={}", config.getKey(), e.getMessage());
            return RetBuilder.error("配置验证失败: " + e.getMessage());
        }
    }

}
