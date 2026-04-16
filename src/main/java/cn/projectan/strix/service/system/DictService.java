package cn.projectan.strix.service.system;

import cn.projectan.strix.mapper.system.DictMapper;
import cn.projectan.strix.model.db.system.Dict;
import cn.projectan.strix.model.db.system.DictChangeLog;
import cn.projectan.strix.model.db.system.DictData;
import cn.projectan.strix.model.dict.common.CommonFlag;
import cn.projectan.strix.model.dict.common.CommonSwitch;
import cn.projectan.strix.model.dict.system.DictChangeType;
import cn.projectan.strix.model.enums.common.NumCategory;
import cn.projectan.strix.model.event.DictChangedEvent;
import cn.projectan.strix.model.request.system.dict.DictCloneReq;
import cn.projectan.strix.model.request.system.dict.DictDataUpdateReq;
import cn.projectan.strix.model.request.system.dict.DictImportReq;
import cn.projectan.strix.model.request.system.dict.DictListReq;
import cn.projectan.strix.model.request.system.dict.DictSortReq;
import cn.projectan.strix.model.request.system.dict.DictUpdateReq;
import cn.projectan.strix.model.response.common.CommonDictResp;
import cn.projectan.strix.model.response.common.CommonDictVersionResp;
import cn.projectan.strix.model.response.system.dict.DictDataListResp;
import cn.projectan.strix.model.response.system.dict.DictExportData;
import cn.projectan.strix.model.response.system.dict.DictSearchResultResp;
import cn.projectan.strix.util.common.I18nUtil;
import cn.projectan.strix.util.common.UniqueChecker;
import cn.projectan.strix.util.common.UpdateBuilder;
import cn.projectan.strix.util.math.NumUtil;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * <p>
 * Strix 字典 服务类
 * </p>
 *
 * @author ProjectAn
 * @since 2021-08-31
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DictService extends ServiceImpl<DictMapper, Dict> {

    private final DictDataService dictDataService;
    private final DictChangeLogService dictChangeLogService;
    private final DictUsageStatService dictUsageStatService;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * 分页查询字典列表
     *
     * @param req 查询请求
     * @return 分页结果
     */
    public Page<Dict> listPage(DictListReq req) {
        return lambdaQuery()
                .like(StringUtils.hasText(req.getKeyword()), Dict::getKey, req.getKeyword())
                .or(StringUtils.hasText(req.getKeyword()), q -> q.like(Dict::getName, req.getKeyword()))
                .eq(NumUtil.checkCategory(req.getStatus(), NumCategory.NON_NEGATIVE), Dict::getStatus, req.getStatus())
                .eq(NumUtil.checkCategory(req.getProvided(), NumCategory.NON_NEGATIVE), Dict::getProvided, req.getProvided())
                .page(req.getPage());
    }

    /**
     * 获取字典版本
     *
     * @return 字典版本
     */
    @Cacheable(value = "strix:dict:versionMap")
    public CommonDictVersionResp getDictVersionMapResp() {
        List<Dict> dictList = lambdaQuery()
                .select(Dict::getKey, Dict::getVersion)
                .eq(Dict::getStatus, CommonSwitch.ENABLE)
                .list();
        return new CommonDictVersionResp(dictList);
    }

    /**
     * 获取字典数据
     *
     * @param key 字典key
     * @return 字典
     */
    @Cacheable(value = "strix:dict:dictResp", key = "#key")
    public CommonDictResp getDictResp(String key) {
        Dict dict = lambdaQuery()
                .eq(Dict::getKey, key)
                .eq(Dict::getStatus, CommonSwitch.ENABLE)
                .one();

        List<DictData> dictDataList = dictDataService.listValidByKey(key);

        if (dict == null || CollectionUtils.isEmpty(dictDataList)) {
            return null;
        }

        dictUsageStatService.incrementAccess(key);

        return new CommonDictResp(
                dict.getId(),
                dict.getKey(),
                dict.getDataType(),
                dict.getVersion(),
                new DictDataListResp(dictDataList, dictDataList.size()).getItems());
    }

    /**
     * 保存字典
     *
     * @param dict 字典
     */
    @Caching(
            evict = {
                    @CacheEvict(value = "strix:dict:dictResp", key = "#dict.key"),
                    @CacheEvict(value = "strix:dict:versionMap", allEntries = true)
            }
    )
    public void saveDict(Dict dict) {
        UniqueChecker.check(dict);
        Assert.isTrue(save(dict), "保存失败");
        dictChangeLogService.record(dict.getKey(), DictChangeType.DICT_CREATED, null, dict, "创建字典");
        eventPublisher.publishEvent(new DictChangedEvent(this, dict.getKey(), "dict_saved"));
    }

    /**
     * 更新字典
     *
     * @param dict 字典
     * @param req  字典更新请求
     */
    @Caching(
            evict = {
                    @CacheEvict(value = "strix:dict:dictResp", key = "#dict.key"),
                    @CacheEvict(value = "strix:dict:versionMap", allEntries = true)
            }
    )
    @Transactional(rollbackFor = Exception.class)
    public void updateDict(Dict dict, DictUpdateReq req) {
        List<DictData> before = dictDataService.listByKey(dict.getKey());

        // 如果key发生变化，需要同步更新dict_data表中的key
        if (StringUtils.hasText(req.getKey()) && !req.getKey().equals(dict.getKey())) {
            dictDataService.lambdaUpdate()
                    .eq(DictData::getKey, dict.getKey())
                    .set(DictData::getKey, req.getKey())
                    .update();
        }

        LambdaUpdateWrapper<Dict> updateWrapper = UpdateBuilder.build(dict, req);
        UniqueChecker.check(dict);
        updateWrapper.set(Dict::getVersion, dict.getVersion() + 1);
        Assert.isTrue(update(updateWrapper), "保存失败");
        String effectiveKey = StringUtils.hasText(req.getKey()) ? req.getKey() : dict.getKey();
        dictChangeLogService.record(effectiveKey, DictChangeType.DICT_UPDATED, before, dict, "修改字典");
        eventPublisher.publishEvent(new DictChangedEvent(this, effectiveKey, "dict_updated"));
    }

    /**
     * 删除字典
     *
     * @param dict 字典
     */
    @Caching(
            evict = {
                    @CacheEvict(value = "strix:dict:dictResp", key = "#dict.key"),
                    @CacheEvict(value = "strix:dict:versionMap", allEntries = true)
            }
    )
    public void deleteDict(Dict dict) {
        checkProvidedProtection(dict, "删除");
        List<DictData> before = dictDataService.listByKey(dict.getKey());
        dictChangeLogService.record(dict.getKey(), DictChangeType.DICT_DELETED, before, null, "删除字典");
        Assert.isTrue(dictDataService.lambdaUpdate()
                .eq(DictData::getKey, dict.getKey())
                .remove(), "删除失败");
        eventPublisher.publishEvent(new DictChangedEvent(this, dict.getKey(), "dict_deleted"));
    }

    /**
     * 保存字典数据
     *
     * @param dictData 字典数据
     */
    @Caching(
            evict = {
                    @CacheEvict(value = "strix:dict:dictResp", key = "#dictData.key"),
                    @CacheEvict(value = "strix:dict:versionMap", allEntries = true)
            }
    )
    @Transactional(rollbackFor = Exception.class)
    public void saveDictData(DictData dictData) {
        UniqueChecker.check(dictData);
        if (dictData.getIsDefault() != null && dictData.getIsDefault() == CommonFlag.YES) {
            long existingDefaults = dictDataService.lambdaQuery()
                    .eq(DictData::getKey, dictData.getKey())
                    .eq(DictData::getIsDefault, CommonFlag.YES)
                    .count();
            Assert.isTrue(existingDefaults == 0, "该字典已有默认值，每个字典最多允许一个默认项");
        }
        incrementDictVersion(dictData.getKey());
        Assert.isTrue(dictDataService.save(dictData), "保存失败");
        dictChangeLogService.record(dictData.getKey(), DictChangeType.DATA_ADDED, null, dictData, "新增数据项");
        eventPublisher.publishEvent(new DictChangedEvent(this, dictData.getKey(), "data_added"));
    }

    /**
     * 更新字典数据
     *
     * @param dictData 字典数据
     * @param req      字典数据更新请求
     */
    @Caching(
            evict = {
                    @CacheEvict(value = "strix:dict:dictResp", key = "#dictData.key"),
                    @CacheEvict(value = "strix:dict:versionMap", allEntries = true)
            }
    )
    @Transactional(rollbackFor = Exception.class)
    public void updateDictData(DictData dictData, DictDataUpdateReq req) {
        DictData before = dictDataService.getById(dictData.getId());
        LambdaUpdateWrapper<DictData> updateWrapper = UpdateBuilder.build(dictData, req);
        UniqueChecker.check(dictData);
        incrementDictVersion(dictData.getKey());
        Assert.isTrue(dictDataService.update(updateWrapper), "保存失败");
        dictChangeLogService.record(dictData.getKey(), DictChangeType.DATA_UPDATED, before, dictData, "修改数据项");
        eventPublisher.publishEvent(new DictChangedEvent(this, dictData.getKey(), "data_updated"));
    }

    /**
     * 按 ID 更新字典数据（用于批量导入覆盖更新）
     *
     * @param dictData 字典数据
     */
    @Caching(
            evict = {
                    @CacheEvict(value = "strix:dict:dictResp", key = "#dictData.key"),
                    @CacheEvict(value = "strix:dict:versionMap", allEntries = true)
            }
    )
    @Transactional(rollbackFor = Exception.class)
    public void updateDictDataById(DictData dictData) {
        incrementDictVersion(dictData.getKey());
        Assert.isTrue(dictDataService.updateById(dictData), "保存失败");
        eventPublisher.publishEvent(new DictChangedEvent(this, dictData.getKey(), "data_updated"));
    }

    /**
     * 删除字典数据
     *
     * @param dictData 字典数据
     */
    @Caching(
            evict = {
                    @CacheEvict(value = "strix:dict:dictResp", key = "#dictData.key"),
                    @CacheEvict(value = "strix:dict:versionMap", allEntries = true)
            }
    )
    @Transactional(rollbackFor = Exception.class)
    public void deleteDictData(DictData dictData) {
        dictChangeLogService.record(dictData.getKey(), DictChangeType.DATA_DELETED, dictData, null, "删除数据项");
        incrementDictVersion(dictData.getKey());

        dictDataService.lambdaUpdate()
                .eq(DictData::getKey, dictData.getKey())
                .eq(DictData::getValue, dictData.getValue())
                .remove();
        eventPublisher.publishEvent(new DictChangedEvent(this, dictData.getKey(), "data_deleted"));
    }

    // ======================== Clone ========================

    @Caching(evict = {
            @CacheEvict(value = "strix:dict:versionMap", allEntries = true)
    })
    @Transactional(rollbackFor = Exception.class)
    public void cloneDict(String sourceDictKey, DictCloneReq req) {
        Dict source = lambdaQuery().eq(Dict::getKey, sourceDictKey).one();
        Assert.notNull(source, I18nUtil.notFound("field.dict"));

        Dict cloned = new Dict()
                .setKey(req.getNewKey())
                .setName(req.getNewName())
                .setDataType(source.getDataType())
                .setStatus(source.getStatus())
                .setRemark(source.getRemark())
                .setVersion(0)
                .setProvided(CommonFlag.NO)
                .setGroupId(source.getGroupId())
                .setParentDictKey(source.getParentDictKey());
        UniqueChecker.check(cloned);
        Assert.isTrue(save(cloned), "保存失败");

        List<DictData> sourceData = dictDataService.listByKey(sourceDictKey);
        List<DictData> clonedData = sourceData.stream().map(d -> new DictData()
                .setKey(req.getNewKey())
                .setValue(d.getValue())
                .setLabel(d.getLabel())
                .setSort(d.getSort())
                .setStyle(d.getStyle())
                .setStatus(d.getStatus())
                .setRemark(d.getRemark())
                .setParentValue(d.getParentValue())
                .setIsDefault(d.getIsDefault())
                .setValidFrom(d.getValidFrom())
                .setValidTo(d.getValidTo())
        ).toList();
        if (!clonedData.isEmpty()) {
            dictDataService.saveBatch(clonedData);
        }

        dictChangeLogService.record(req.getNewKey(), DictChangeType.DICT_CLONED, null, clonedData, "从 " + sourceDictKey + " 克隆");
        eventPublisher.publishEvent(new DictChangedEvent(this, req.getNewKey(), "dict_cloned"));
    }

    // ======================== Sort ========================

    @Caching(evict = {
            @CacheEvict(value = "strix:dict:dictResp", key = "#dictKey"),
            @CacheEvict(value = "strix:dict:versionMap", allEntries = true)
    })
    @Transactional(rollbackFor = Exception.class)
    public void batchSort(String dictKey, DictSortReq req) {
        List<DictData> before = dictDataService.listByKey(dictKey);
        for (int i = 0; i < req.getSortedIds().size(); i++) {
            dictDataService.lambdaUpdate()
                    .eq(DictData::getId, req.getSortedIds().get(i))
                    .set(DictData::getSort, (short) i)
                    .update();
        }
        incrementDictVersion(dictKey);
        List<DictData> after = dictDataService.listByKey(dictKey);
        dictChangeLogService.record(dictKey, DictChangeType.DATA_SORTED, before, after, "排序更新");
        eventPublisher.publishEvent(new DictChangedEvent(this, dictKey, "data_sorted"));
    }

    // ======================== Global Search ========================

    public DictSearchResultResp globalSearch(String keyword) {
        List<DictSearchResultResp.SearchResultItem> results = new ArrayList<>();

        List<Dict> matchedDicts = lambdaQuery()
                .like(Dict::getKey, keyword)
                .or().like(Dict::getName, keyword)
                .last("LIMIT 25")
                .list();
        for (Dict d : matchedDicts) {
            if (d.getKey().toLowerCase().contains(keyword.toLowerCase())) {
                results.add(new DictSearchResultResp.SearchResultItem(d.getKey(), d.getName(), "DICT_KEY", "key", d.getKey()));
            }
            if (d.getName().contains(keyword)) {
                results.add(new DictSearchResultResp.SearchResultItem(d.getKey(), d.getName(), "DICT_NAME", "name", d.getName()));
            }
        }

        List<DictData> matchedData = dictDataService.lambdaQuery()
                .like(DictData::getLabel, keyword)
                .or().like(DictData::getValue, keyword)
                .last("LIMIT 25")
                .list();
        for (DictData dd : matchedData) {
            Dict parentDict = lambdaQuery().eq(Dict::getKey, dd.getKey()).one();
            String dictName = parentDict != null ? parentDict.getName() : dd.getKey();
            if (dd.getLabel() != null && dd.getLabel().contains(keyword)) {
                results.add(new DictSearchResultResp.SearchResultItem(dd.getKey(), dictName, "DATA_LABEL", "label", dd.getLabel()));
            }
            if (dd.getValue() != null && dd.getValue().contains(keyword)) {
                results.add(new DictSearchResultResp.SearchResultItem(dd.getKey(), dictName, "DATA_VALUE", "value", dd.getValue()));
            }
        }

        DictSearchResultResp resp = new DictSearchResultResp();
        resp.setItems(results.stream().limit(50).toList());
        return resp;
    }

    // ======================== Export / Import ========================

    public List<DictExportData> exportDicts(List<String> dictKeys) {
        List<DictExportData> result = new ArrayList<>();
        for (String key : dictKeys) {
            Dict dict = lambdaQuery().eq(Dict::getKey, key).one();
            if (dict == null) continue;
            List<DictData> dataList = dictDataService.listByKey(key);
            List<DictExportData.ExportDataItem> items = dataList.stream()
                    .map(d -> new DictExportData.ExportDataItem(
                            d.getValue(), d.getLabel(), d.getSort(), d.getStyle(),
                            d.getStatus(), d.getRemark(), d.getParentValue(), d.getIsDefault()))
                    .toList();
            result.add(new DictExportData(dict.getKey(), dict.getName(), dict.getDataType(), dict.getRemark(), items));
        }
        return result;
    }

    @Transactional(rollbackFor = Exception.class)
    public void importDicts(DictImportReq req) {
        for (DictExportData dictData : req.getDicts()) {
            Dict existing = lambdaQuery().eq(Dict::getKey, dictData.getKey()).one();

            if (existing != null) {
                switch (req.getConflictStrategy()) {
                    case "SKIP" -> { continue; }
                    case "OVERWRITE" -> {
                        dictDataService.lambdaUpdate().eq(DictData::getKey, dictData.getKey()).remove();
                    }
                    case "RENAME" -> {
                        dictData.setKey(dictData.getKey() + "_imported");
                        existing = null;
                    }
                }
            }

            if (existing == null) {
                Dict newDict = new Dict()
                        .setKey(dictData.getKey())
                        .setName(dictData.getName())
                        .setDataType(dictData.getDataType())
                        .setStatus(CommonSwitch.ENABLE)
                        .setRemark(dictData.getRemark())
                        .setVersion(0)
                        .setProvided(CommonFlag.NO);
                save(newDict);
            }

            if (dictData.getItems() != null) {
                List<DictData> dataItems = dictData.getItems().stream()
                        .map(item -> new DictData()
                                .setKey(dictData.getKey())
                                .setValue(item.getValue())
                                .setLabel(item.getLabel())
                                .setSort(item.getSort())
                                .setStyle(item.getStyle())
                                .setStatus(item.getStatus())
                                .setRemark(item.getRemark())
                                .setParentValue(item.getParentValue())
                                .setIsDefault(item.getIsDefault()))
                        .toList();
                if (!dataItems.isEmpty()) {
                    dictDataService.saveBatch(dataItems);
                }
            }

            incrementDictVersion(dictData.getKey());
            dictChangeLogService.record(dictData.getKey(), DictChangeType.DICT_IMPORTED, null, dictData, "导入");
            eventPublisher.publishEvent(new DictChangedEvent(this, dictData.getKey(), "dict_imported"));
        }
    }

    // ======================== Rollback ========================

    @Caching(evict = {
            @CacheEvict(value = "strix:dict:dictResp", key = "#dictKey"),
            @CacheEvict(value = "strix:dict:versionMap", allEntries = true)
    })
    @Transactional(rollbackFor = Exception.class)
    public void rollbackToSnapshot(String dictKey, String logId) {
        DictChangeLog logEntry = dictChangeLogService.getLog(logId);
        Assert.notNull(logEntry, "变更记录不存在");
        Assert.hasText(logEntry.getSnapshotAfter(), "该记录没有可回滚的快照");

        List<DictData> snapshot = dictChangeLogService.deserializeSnapshot(logEntry.getSnapshotAfter());
        List<DictData> currentData = dictDataService.listByKey(dictKey);

        dictDataService.lambdaUpdate().eq(DictData::getKey, dictKey).remove();

        if (!snapshot.isEmpty()) {
            List<DictData> newItems = snapshot.stream().map(d -> new DictData()
                    .setKey(dictKey)
                    .setValue(d.getValue())
                    .setLabel(d.getLabel())
                    .setSort(d.getSort())
                    .setStyle(d.getStyle())
                    .setStatus(d.getStatus())
                    .setRemark(d.getRemark())
                    .setParentValue(d.getParentValue())
                    .setIsDefault(d.getIsDefault())
                    .setValidFrom(d.getValidFrom())
                    .setValidTo(d.getValidTo())
            ).toList();
            dictDataService.saveBatch(newItems);
        }

        incrementDictVersion(dictKey);
        dictChangeLogService.record(dictKey, DictChangeType.DICT_UPDATED, currentData, snapshot, "回滚至快照 " + logId);
        eventPublisher.publishEvent(new DictChangedEvent(this, dictKey, "dict_rollback"));
    }

    // ======================== Protection ========================

    private void checkProvidedProtection(Dict dict, String operation) {
        if (dict.getProvided() != null && dict.getProvided() == CommonFlag.YES) {
            throw new IllegalArgumentException("内置字典不可" + operation);
        }
    }

    private void incrementDictVersion(String dictKey) {
        boolean updated = lambdaUpdate()
                .eq(Dict::getKey, dictKey)
                .setSql("version = version + 1")
                .update();
        Assert.isTrue(updated, I18nUtil.notFound("field.dict"));
    }


}
