package cn.projectan.strix.controller.system;

import cn.projectan.strix.controller.system.base.BaseSystemController;
import cn.projectan.strix.core.ret.RetMarker;
import cn.projectan.strix.core.ret.RetResult;
import cn.projectan.strix.core.validation.group.InsertGroup;
import cn.projectan.strix.core.validation.group.UpdateGroup;
import cn.projectan.strix.model.annotation.StrixLog;
import cn.projectan.strix.model.db.Dict;
import cn.projectan.strix.model.db.DictData;
import cn.projectan.strix.model.dict.DictDataStatus;
import cn.projectan.strix.model.dict.DictProvided;
import cn.projectan.strix.model.dict.DictStatus;
import cn.projectan.strix.model.dict.SysLogOperType;
import cn.projectan.strix.model.request.system.dict.DictDataListReq;
import cn.projectan.strix.model.request.system.dict.DictDataUpdateReq;
import cn.projectan.strix.model.request.system.dict.DictListReq;
import cn.projectan.strix.model.request.system.dict.DictUpdateReq;
import cn.projectan.strix.model.response.system.dict.DictDataListResp;
import cn.projectan.strix.model.response.system.dict.DictDataResp;
import cn.projectan.strix.model.response.system.dict.DictListResp;
import cn.projectan.strix.model.response.system.dict.DictResp;
import cn.projectan.strix.service.DictDataService;
import cn.projectan.strix.service.DictService;
import cn.projectan.strix.utils.NumUtils;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * @author 安炯奕
 * @date 2022/4/4 23:43
 */
@Slf4j
@RestController
@RequestMapping("system/dict")
@RequiredArgsConstructor
public class SystemDictController extends BaseSystemController {

    private final DictService dictService;
    private final DictDataService dictDataService;

    @GetMapping("")
    @PreAuthorize("@ss.hasPermission('system:dict')")
    @StrixLog(operationGroup = "系统字典", operationName = "查询字典列表")
    public RetResult<DictListResp> list(DictListReq req) {
        LambdaQueryWrapper<Dict> queryWrapper = new LambdaQueryWrapper<>();

        if (StringUtils.hasText(req.getKeyword())) {
            queryWrapper.like(Dict::getKey, req.getKeyword())
                    .or()
                    .like(Dict::getName, req.getKeyword());
        }
        if (NumUtils.isPositiveNumber(req.getStatus())) {
            queryWrapper.eq(Dict::getStatus, req.getStatus());
        }
        if (NumUtils.isPositiveNumber(req.getProvided())) {
            queryWrapper.eq(Dict::getProvided, req.getProvided());
        }

        Page<Dict> page = dictService.page(req.getPage(), queryWrapper);

        return RetMarker.makeSuccessRsp(
                new DictListResp(page.getRecords(), page.getTotal())
        );
    }

    @GetMapping("{id}")
    @PreAuthorize("@ss.hasPermission('system:dict')")
    @StrixLog(operationGroup = "系统字典", operationName = "查询字典信息")
    public RetResult<DictResp> info(@PathVariable String id) {
        Dict dict = dictService.getById(id);
        Assert.notNull(dict, "该数据不存在");

        List<DictData> dictDataList = dictDataService.lambdaQuery()
                .eq(DictData::getKey, dict.getKey()).list();
        List<DictDataListResp.DictDataItem> dictDataItems = new DictDataListResp(dictDataList, dictDataList.size()).getItems();

        return RetMarker.makeSuccessRsp(
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

    @PostMapping("update")
    @PreAuthorize("@ss.hasPermission('system:dict:add')")
    @StrixLog(operationGroup = "系统字典", operationName = "新增字典", operationType = SysLogOperType.ADD)
    public RetResult<Object> update(@RequestBody @Validated(InsertGroup.class) DictUpdateReq req) {
        Assert.isTrue(DictStatus.valid(req.getStatus()), "请选择正确的字典状态");

        Dict dict = new Dict(
                req.getKey(),
                req.getName(),
                req.getDataType(),
                req.getStatus(),
                req.getRemark(),
                0,
                DictProvided.NO
        );
        dict.setCreateBy(getLoginManagerId());
        dict.setUpdateBy(getLoginManagerId());

        dictService.saveDict(dict);

        return RetMarker.makeSuccessRsp();
    }

    @PostMapping("update/{id}")
    @PreAuthorize("@ss.hasPermission('system:dict:update')")
    @StrixLog(operationGroup = "系统字典", operationName = "修改字典", operationType = SysLogOperType.UPDATE)
    public RetResult<Object> update(@PathVariable String id, @RequestBody @Validated(UpdateGroup.class) DictUpdateReq req) {
        Assert.isTrue(DictStatus.valid(req.getStatus()), "请选择正确的字典状态");

        Dict dict = dictService.getById(id);
        Assert.notNull(dict, "原数据不存在");

        dictService.updateDict(dict, req);

        return RetMarker.makeSuccessRsp();
    }

    @PostMapping("remove/{id}")
    @PreAuthorize("@ss.hasPermission('system:dict:remove')")
    @StrixLog(operationGroup = "系统字典", operationName = "删除字典", operationType = SysLogOperType.DELETE)
    public RetResult<Object> remove(@PathVariable String id) {
        Assert.hasText(id, "参数错误");

        Dict dict = dictService.getById(id);
        if (dict != null) {
            dictService.removeDict(dict);
        }

        return RetMarker.makeSuccessRsp();
    }

    @GetMapping("data/{key}")
    @PreAuthorize("@ss.hasPermission('system:dict:data')")
    @StrixLog(operationGroup = "系统字典", operationName = "查询字典数据列表")
    public RetResult<DictDataListResp> getDictDataList(@PathVariable String key, DictDataListReq req) {
        LambdaQueryWrapper<DictData> queryWrapper = new LambdaQueryWrapper<>();

        queryWrapper.eq(DictData::getKey, key);
        if (StringUtils.hasText(req.getKeyword())) {
            queryWrapper.like(DictData::getValue, req.getKeyword())
                    .or()
                    .like(DictData::getLabel, req.getKeyword());
        }
        if (NumUtils.isPositiveNumber(req.getStatus())) {
            queryWrapper.eq(DictData::getStatus, req.getStatus());
        }
        queryWrapper.orderByAsc(DictData::getSort);

        Page<DictData> page = dictDataService.page(req.getPage(), queryWrapper);

        return RetMarker.makeSuccessRsp(
                new DictDataListResp(page.getRecords(), page.getTotal())
        );
    }

    @GetMapping("data/{key}/{id}")
    @PreAuthorize("@ss.hasPermission('system:dict:data')")
    @StrixLog(operationGroup = "系统字典", operationName = "查询字典数据信息")
    public RetResult<DictDataResp> getDictDataInfo(@PathVariable String key, @PathVariable String id) {
        DictData dictData = dictDataService.getById(id);
        Assert.notNull(dictData, "该数据不存在");

        return RetMarker.makeSuccessRsp(
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

    @PostMapping("data/{key}/update")
    @PreAuthorize("@ss.hasPermission('system:dict:data:add')")
    @StrixLog(operationGroup = "系统字典", operationName = "新增字典", operationType = SysLogOperType.ADD)
    public RetResult<Object> updateDictData(@RequestBody @Validated(InsertGroup.class) DictDataUpdateReq req) {
        Assert.isTrue(DictDataStatus.valid(req.getStatus()), "请选择正确的字典状态");

        DictData dictData = new DictData(
                req.getKey(),
                req.getValue(),
                req.getLabel(),
                req.getSort(),
                req.getStyle(),
                req.getStatus(),
                req.getRemark()
        );
        dictData.setCreateBy(getLoginManagerId());
        dictData.setUpdateBy(getLoginManagerId());

        dictService.saveDictData(dictData);

        return RetMarker.makeSuccessRsp();
    }

    @PostMapping("data/{key}/update/{id}")
    @PreAuthorize("@ss.hasPermission('system:dict:data:update')")
    @StrixLog(operationGroup = "系统字典", operationName = "修改字典数据", operationType = SysLogOperType.UPDATE)
    public RetResult<Object> updateDictData(@PathVariable String id, @RequestBody @Validated(UpdateGroup.class) DictDataUpdateReq req) {
        Assert.isTrue(DictDataStatus.valid(req.getStatus()), "请选择正确的字典状态");

        DictData dictData = dictDataService.getById(id);
        Assert.notNull(dictData, "原数据不存在");

        dictService.updateDictData(dictData, req);

        return RetMarker.makeSuccessRsp();
    }

    @PostMapping("data/{key}/remove/{id}")
    @PreAuthorize("@ss.hasPermission('system:dict:data:remove')")
    @StrixLog(operationGroup = "系统字典", operationName = "删除字典数据", operationType = SysLogOperType.DELETE)
    public RetResult<Object> removeDictData(@PathVariable String id) {
        Assert.hasText(id, "参数错误");

        DictData dictData = dictDataService.getById(id);
        if (dictData != null) {
            dictService.removeDictData(dictData);
        }

        return RetMarker.makeSuccessRsp();
    }

}
