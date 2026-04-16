package cn.projectan.strix.controller.system;

import cn.projectan.strix.controller.system.base.BaseSystemController;
import cn.projectan.strix.core.exception.StrixUniqueCheckerException;
import cn.projectan.strix.core.ret.RetBuilder;
import cn.projectan.strix.core.ret.RetResult;
import cn.projectan.strix.core.validation.group.InsertGroup;
import cn.projectan.strix.core.validation.group.UpdateGroup;
import cn.projectan.strix.model.annotation.StrixLog;
import cn.projectan.strix.model.db.system.Dict;
import cn.projectan.strix.model.db.system.DictData;
import cn.projectan.strix.model.dict.common.CommonFlag;
import cn.projectan.strix.model.dict.common.CommonSwitch;
import cn.projectan.strix.model.dict.system.SystemLogOperType;
import cn.projectan.strix.model.enums.common.DuplicateStrategy;
import cn.projectan.strix.model.request.common.BatchImportReq;
import cn.projectan.strix.model.request.common.BatchModifyReq;
import cn.projectan.strix.model.request.common.BatchRemoveReq;
import cn.projectan.strix.model.request.system.dict.DictDataListReq;
import cn.projectan.strix.model.request.system.dict.DictDataUpdateReq;
import cn.projectan.strix.model.request.system.dict.DictListReq;
import cn.projectan.strix.model.request.system.dict.DictUpdateReq;
import cn.projectan.strix.model.response.common.BatchImportResp;
import cn.projectan.strix.model.response.common.BatchImportResp.ImportError;
import cn.projectan.strix.model.response.system.dict.DictDataListResp;
import cn.projectan.strix.model.response.system.dict.DictDataResp;
import cn.projectan.strix.model.response.system.dict.DictListResp;
import cn.projectan.strix.model.response.system.dict.DictResp;
import cn.projectan.strix.service.system.DictDataService;
import cn.projectan.strix.service.system.DictService;
import cn.projectan.strix.util.common.I18nUtil;
import cn.projectan.strix.util.common.ObjectMapperUtil;
import cn.projectan.strix.util.common.UniqueChecker;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.util.Assert;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 系统字典
 *
 * @author ProjectAn
 * @since 2022/4/4 23:43
 */
@Slf4j
@RestController
@RequestMapping("system/dict")
@RequiredArgsConstructor
@Tag(name = "系统 - 字典管理")
public class SystemDictController extends BaseSystemController {

    private final DictService dictService;
    private final DictDataService dictDataService;
    private final Validator validator;

    /**
     * 查询字典列表
     */
    @Operation(summary = "字典列表")
    @GetMapping("")
    @PreAuthorize("@ss.hasPermission('system:dict')")
    @StrixLog(operationGroup = "系统字典", operationName = "查询字典列表")
    public RetResult<DictListResp> list(DictListReq req) {
        Page<Dict> page = dictService.listPage(req);

        return RetBuilder.success(
                new DictListResp(page.getRecords(), page.getTotal())
        );
    }

    /**
     * 查询字典信息
     */
    @Operation(summary = "字典详情")
    @GetMapping("{id}")
    @PreAuthorize("@ss.hasPermission('system:dict')")
    @StrixLog(operationGroup = "系统字典", operationName = "查询字典信息")
    public RetResult<DictResp> info(@Parameter(description = "字典 ID") @PathVariable String id) {
        Dict dict = dictService.getById(id);
        Assert.notNull(dict, I18nUtil.notFound("field.dictItem"));

        List<DictData> dictDataList = dictDataService.listByKey(dict.getKey());
        List<DictDataListResp.DictDataItem> dictDataItems = new DictDataListResp(dictDataList, dictDataList.size()).getItems();

        return RetBuilder.success(
                new DictResp(
                        dict.getId(),
                        dict.getKey(),
                        dict.getName(),
                        dict.getDataType(),
                        dict.getStatus(),
                        dict.getRemark(),
                        dict.getVersion(),
                        dict.getProvided(),
                        dictDataItems
                )
        );
    }

    /**
     * 新增字典
     */
    @Operation(summary = "新增字典")
    @PostMapping("update")
    @PreAuthorize("@ss.hasPermission('system:dict:add')")
    @StrixLog(operationGroup = "系统字典", operationName = "新增字典", operationType = SystemLogOperType.ADD)
    public RetResult<Void> update(@RequestBody @Validated(InsertGroup.class) DictUpdateReq req) {
        Dict dict = new Dict(
                req.getKey(),
                req.getName(),
                req.getDataType(),
                req.getStatus(),
                req.getRemark(),
                0,
                CommonFlag.NO
        );

        dictService.saveDict(dict);

        return RetBuilder.success();
    }

    /**
     * 修改字典
     */
    @Operation(summary = "编辑字典")
    @PostMapping("update/{id}")
    @PreAuthorize("@ss.hasPermission('system:dict:update')")
    @StrixLog(operationGroup = "系统字典", operationName = "修改字典", operationType = SystemLogOperType.UPDATE)
    public RetResult<Void> update(@Parameter(description = "字典 ID") @PathVariable String id, @RequestBody @Validated(UpdateGroup.class) DictUpdateReq req) {
        Dict dict = dictService.getById(id);
        Assert.notNull(dict, I18nUtil.notFound("field.originalData"));

        dictService.updateDict(dict, req);

        return RetBuilder.success();
    }

    /**
     * 删除字典
     */
    @Operation(summary = "删除字典")
    @PostMapping("remove/{id}")
    @PreAuthorize("@ss.hasPermission('system:dict:remove')")
    @StrixLog(operationGroup = "系统字典", operationName = "删除字典", operationType = SystemLogOperType.DELETE)
    public RetResult<Void> remove(@Parameter(description = "字典 ID") @PathVariable String id) {
        Assert.hasText(id, I18nUtil.get("error.param.invalid"));

        Dict dict = dictService.getById(id);
        if (dict != null) {
            dictService.deleteDict(dict);
        }

        return RetBuilder.success();
    }

    /**
     * 批量删除字典
     */
    @Operation(summary = "批量删除字典")
    @PostMapping("batch/remove")
    @PreAuthorize("@ss.hasPermission('system:dict:remove')")
    @StrixLog(operationGroup = "系统字典", operationName = "批量删除字典", operationType = SystemLogOperType.DELETE)
    public RetResult<Void> batchRemove(@RequestBody @Validated BatchRemoveReq req) {
        List<Dict> dicts = dictService.listByIds(req.getIds());
        Assert.notEmpty(dicts, I18nUtil.notFound("field.dictItem"));

        List<Dict> removable = dicts.stream()
                .filter(d -> d.getProvided() == CommonFlag.NO)
                .toList();
        Assert.notEmpty(removable, "内置字典不允许删除");

        for (Dict d : removable) {
            dictService.deleteDict(d);
        }

        return RetBuilder.success();
    }

    /**
     * 批量修改字典字段
     */
    @Operation(summary = "批量修改字典字段")
    @PostMapping("batch/modify")
    @PreAuthorize("@ss.hasPermission('system:dict:update')")
    @StrixLog(operationGroup = "系统字典", operationName = "批量修改字典字段", operationType = SystemLogOperType.UPDATE)
    public RetResult<Void> batchModify(@RequestBody @Validated BatchModifyReq req) {
        Assert.hasText(req.getField(), "参数错误");

        switch (req.getField()) {
            case "status" -> {
                Assert.isTrue(CommonSwitch.valid(Short.parseShort(req.getValue())), "参数错误");
                dictService.lambdaUpdate()
                        .in(Dict::getId, req.getIds())
                        .eq(Dict::getProvided, CommonFlag.NO)
                        .set(Dict::getStatus, req.getValue())
                        .update();
            }
            default -> {
                return RetBuilder.error(I18nUtil.get("error.param.invalid"));
            }
        }

        return RetBuilder.success();
    }

    /**
     * 查询字典数据列表
     */
    @Operation(summary = "字典数据列表")
    @GetMapping("data/{key}")
    @PreAuthorize("@ss.hasPermission('system:dict:data')")
    @StrixLog(operationGroup = "系统字典", operationName = "查询字典数据列表")
    public RetResult<DictDataListResp> getDictDataList(@Parameter(description = "字典 Key") @PathVariable String key, DictDataListReq req) {
        Page<DictData> page = dictDataService.listPage(key, req);

        return RetBuilder.success(
                new DictDataListResp(page.getRecords(), page.getTotal())
        );
    }

    /**
     * 查询字典数据信息
     */
    @Operation(summary = "字典数据详情")
    @GetMapping("data/{key}/{id}")
    @PreAuthorize("@ss.hasPermission('system:dict:data')")
    @StrixLog(operationGroup = "系统字典", operationName = "查询字典数据信息")
    public RetResult<DictDataResp> getDictDataInfo(@Parameter(description = "字典 Key") @PathVariable String key, @Parameter(description = "字典数据 ID") @PathVariable String id) {
        DictData dictData = dictDataService.getById(id);
        Assert.notNull(dictData, I18nUtil.notFound("field.dictData"));

        return RetBuilder.success(
                new DictDataResp(
                        dictData.getId(),
                        dictData.getKey(),
                        dictData.getValue(),
                        dictData.getLabel(),
                        dictData.getSort(),
                        dictData.getStyle(),
                        dictData.getStatus(),
                        dictData.getRemark()
                )
        );
    }

    /**
     * 新增字典数据
     */
    @Operation(summary = "新增字典数据")
    @PostMapping("data/{key}/update")
    @PreAuthorize("@ss.hasPermission('system:dict:data:add')")
    @StrixLog(operationGroup = "系统字典", operationName = "新增字典数据", operationType = SystemLogOperType.ADD)
    public RetResult<Void> updateDictData(@RequestBody @Validated(InsertGroup.class) DictDataUpdateReq req) {
        DictData dictData = new DictData(
                req.getKey(),
                req.getValue(),
                req.getLabel(),
                req.getSort(),
                req.getStyle(),
                req.getStatus(),
                req.getRemark()
        );

        dictService.saveDictData(dictData);

        return RetBuilder.success();
    }

    /**
     * 修改字典数据
     */
    @Operation(summary = "编辑字典数据")
    @PostMapping("data/{key}/update/{id}")
    @PreAuthorize("@ss.hasPermission('system:dict:data:update')")
    @StrixLog(operationGroup = "系统字典", operationName = "修改字典数据", operationType = SystemLogOperType.UPDATE)
    public RetResult<Void> updateDictData(@Parameter(description = "字典数据 ID") @PathVariable String id, @RequestBody @Validated(UpdateGroup.class) DictDataUpdateReq req) {
        DictData dictData = dictDataService.getById(id);
        Assert.notNull(dictData, I18nUtil.notFound("field.originalData"));

        dictService.updateDictData(dictData, req);

        return RetBuilder.success();
    }

    /**
     * 删除字典数据
     */
    @Operation(summary = "删除字典数据")
    @PostMapping("data/{key}/remove/{id}")
    @PreAuthorize("@ss.hasPermission('system:dict:data:remove')")
    @StrixLog(operationGroup = "系统字典", operationName = "删除字典数据", operationType = SystemLogOperType.DELETE)
    public RetResult<Void> removeDictData(@Parameter(description = "字典数据 ID") @PathVariable String id) {
        Assert.hasText(id, I18nUtil.get("error.param.invalid"));

        DictData dictData = dictDataService.getById(id);
        if (dictData != null) {
            dictService.deleteDictData(dictData);
        }

        return RetBuilder.success();
    }

    /**
     * 批量删除字典数据
     */
    @Operation(summary = "批量删除字典数据")
    @PostMapping("data/{key}/batch/remove")
    @PreAuthorize("@ss.hasPermission('system:dict:data:remove')")
    @StrixLog(operationGroup = "系统字典", operationName = "批量删除字典数据", operationType = SystemLogOperType.DELETE)
    public RetResult<Void> batchRemoveDictData(@PathVariable String key, @RequestBody @Validated BatchRemoveReq req) {
        List<DictData> dataList = dictDataService.listByIds(req.getIds());
        Assert.notEmpty(dataList, I18nUtil.notFound("field.dictData"));

        for (DictData dictData : dataList) {
            dictService.deleteDictData(dictData);
        }

        return RetBuilder.success();
    }

    /**
     * 批量修改字典数据字段
     */
    @Operation(summary = "批量修改字典数据字段")
    @PostMapping("data/{key}/batch/modify")
    @PreAuthorize("@ss.hasPermission('system:dict:data:update')")
    @StrixLog(operationGroup = "系统字典", operationName = "批量修改字典数据字段", operationType = SystemLogOperType.UPDATE)
    public RetResult<Void> batchModifyDictData(@PathVariable String key, @RequestBody @Validated BatchModifyReq req) {
        Assert.hasText(req.getField(), "参数错误");

        switch (req.getField()) {
            case "status" -> {
                Assert.isTrue(CommonSwitch.valid(Short.parseShort(req.getValue())), "参数错误");
                dictDataService.lambdaUpdate()
                        .in(DictData::getId, req.getIds())
                        .set(DictData::getStatus, req.getValue())
                        .update();
            }
            default -> {
                return RetBuilder.error(I18nUtil.get("error.param.invalid"));
            }
        }

        return RetBuilder.success();
    }

    /**
     * 批量导入字典数据
     */
    @Operation(summary = "批量导入字典数据")
    @PostMapping("data/{key}/batch/create")
    @PreAuthorize("@ss.hasPermission('system:dict:data:add')")
    @StrixLog(operationGroup = "系统字典", operationName = "批量导入字典数据", operationType = SystemLogOperType.ADD)
    public RetResult<BatchImportResp> dataBatchCreate(
            @Parameter(description = "字典 Key") @PathVariable String key,
            @RequestBody @Validated BatchImportReq req) {
        DuplicateStrategy strategy = DuplicateStrategy.fromString(req.getDuplicateStrategy());
        List<ImportError> errors = new ArrayList<>();
        int successCount = 0;
        int skippedCount = 0;

        for (int i = 0; i < req.getItems().size(); i++) {
            Map<String, Object> itemMap = req.getItems().get(i);
            itemMap.put("key", key);
            try {
                DictDataUpdateReq itemReq = ObjectMapperUtil.get().convertValue(itemMap, DictDataUpdateReq.class);

                Set<ConstraintViolation<DictDataUpdateReq>> violations = validator.validate(itemReq, InsertGroup.class);
                if (!violations.isEmpty()) {
                    for (ConstraintViolation<DictDataUpdateReq> v : violations) {
                        errors.add(new ImportError(i, v.getPropertyPath().toString(), v.getMessage()));
                    }
                    continue;
                }

                DictData existing = dictDataService.lambdaQuery()
                        .eq(DictData::getKey, key)
                        .eq(DictData::getValue, itemReq.getValue())
                        .one();

                if (existing != null) {
                    if (strategy == DuplicateStrategy.SKIP) {
                        skippedCount++;
                        errors.add(new ImportError(i, "value", "字典值已存在，已跳过"));
                        continue;
                    }
                    existing.setLabel(itemReq.getLabel());
                    existing.setSort(itemReq.getSort());
                    existing.setStyle(itemReq.getStyle());
                    existing.setStatus(itemReq.getStatus());
                    existing.setRemark(itemReq.getRemark());
                    try {
                        UniqueChecker.check(existing);
                    } catch (StrixUniqueCheckerException e) {
                        errors.add(new ImportError(i, "unique", e.getMessage()));
                        continue;
                    }
                    dictService.updateDictDataById(existing);
                    successCount++;
                    continue;
                }

                DictData dictData = new DictData(
                        key,
                        itemReq.getValue(),
                        itemReq.getLabel(),
                        itemReq.getSort(),
                        itemReq.getStyle(),
                        itemReq.getStatus(),
                        itemReq.getRemark()
                );
                try {
                    dictService.saveDictData(dictData);
                } catch (StrixUniqueCheckerException e) {
                    errors.add(new ImportError(i, "unique", e.getMessage()));
                    continue;
                }
                successCount++;

            } catch (Exception e) {
                errors.add(new ImportError(i, "general", e.getMessage()));
            }
        }

        int failedCount = req.getItems().size() - successCount - skippedCount;
        return RetBuilder.success(new BatchImportResp(req.getItems().size(), successCount, failedCount, skippedCount, errors));
    }

}
