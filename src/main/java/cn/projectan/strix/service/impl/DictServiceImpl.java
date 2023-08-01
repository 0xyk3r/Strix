package cn.projectan.strix.service.impl;

import cn.projectan.strix.mapper.DictMapper;
import cn.projectan.strix.model.db.Dict;
import cn.projectan.strix.model.db.DictData;
import cn.projectan.strix.model.dict.DictStatus;
import cn.projectan.strix.model.request.system.dict.DictDataUpdateReq;
import cn.projectan.strix.model.request.system.dict.DictUpdateReq;
import cn.projectan.strix.model.response.common.CommonDictResp;
import cn.projectan.strix.model.response.common.CommonDictVersionResp;
import cn.projectan.strix.model.response.system.dict.DictDataListResp;
import cn.projectan.strix.service.DictDataService;
import cn.projectan.strix.service.DictService;
import cn.projectan.strix.utils.SecurityUtils;
import cn.projectan.strix.utils.UniqueDetectionTool;
import cn.projectan.strix.utils.UpdateConditionBuilder;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * <p>
 * 服务实现类
 * </p>
 *
 * @author 安炯奕
 * @since 2021-08-31
 */
@Service
@RequiredArgsConstructor
public class DictServiceImpl extends ServiceImpl<DictMapper, Dict> implements DictService {

    private final DictDataService dictDataService;

    @Override
    @Cacheable(value = "strix:dict:versionMap")
    public CommonDictVersionResp getDictVersionMapResp() {
        List<Dict> dictList = list(new LambdaQueryWrapper<>(Dict.class)
                .select(Dict::getKey, Dict::getVersion)
                .eq(Dict::getStatus, DictStatus.ENABLE)
        );
        return new CommonDictVersionResp(dictList);
    }

    @Override
    @Cacheable(value = "strix:dict:dictResp", key = "#key")
    public CommonDictResp getDictResp(String key) {
        Dict dict = getOne(new LambdaQueryWrapper<>(Dict.class)
                .eq(Dict::getKey, key)
                .eq(Dict::getStatus, DictStatus.ENABLE));

        List<DictData> dictDataList = dictDataService.list(new LambdaQueryWrapper<>(DictData.class)
                .eq(DictData::getKey, key)
                .eq(DictData::getStatus, DictStatus.ENABLE)
                .orderByAsc(DictData::getSort));

        if (dict == null || dictDataList == null || dictDataList.isEmpty()) {
            return null;
        }

        return new CommonDictResp(
                dict.getId(),
                dict.getKey(),
                dict.getDataType(),
                dict.getVersion(),
                new DictDataListResp(dictDataList, dictDataList.size()).getItems());
    }

    @Override
    @Caching(
            evict = {
                    @CacheEvict(value = "strix:dict:dictResp", key = "#dict.key"),
                    @CacheEvict(value = "strix:dict:versionMap", allEntries = true)
            }
    )
    public void saveDict(Dict dict) {
        UniqueDetectionTool.check(dict);
        Assert.isTrue(save(dict), "保存失败");
    }

    @Override
    @Caching(
            evict = {
                    @CacheEvict(value = "strix:dict:dictResp", key = "#dict.key"),
                    @CacheEvict(value = "strix:dict:versionMap", allEntries = true)
            }
    )
    @Transactional(rollbackFor = Exception.class)
    public void updateDict(Dict dict, DictUpdateReq req) {
        // 如果key发生变化，需要同步更新dict_data表中的key
        if (StringUtils.hasText(req.getKey()) && !req.getKey().equals(dict.getKey())) {
            dictDataService.lambdaUpdate()
                    .eq(DictData::getKey, dict.getKey())
                    .set(DictData::getKey, req.getKey())
                    .update();
        }

        UpdateWrapper<Dict> updateWrapper = UpdateConditionBuilder.build(dict, req, SecurityUtils.getUserId());
        UniqueDetectionTool.check(dict);
        updateWrapper.set("version", dict.getVersion() + 1);
        Assert.isTrue(update(updateWrapper), "保存失败");
    }

    @Override
    @Caching(
            evict = {
                    @CacheEvict(value = "strix:dict:dictResp", key = "#dict.key"),
                    @CacheEvict(value = "strix:dict:versionMap", allEntries = true)
            }
    )
    public void removeDict(Dict dict) {
        Assert.isTrue(dictDataService.lambdaUpdate()
                .eq(DictData::getKey, dict.getKey())
                .remove(), "删除失败");

        dictDataService.lambdaUpdate()
                .eq(DictData::getKey, dict.getKey())
                .remove();
    }

    @Override
    @Caching(
            evict = {
                    @CacheEvict(value = "strix:dict:dictResp", key = "#dictData.key"),
                    @CacheEvict(value = "strix:dict:versionMap", allEntries = true)
            }
    )
    @Transactional(rollbackFor = Exception.class)
    public void saveDictData(DictData dictData) {
        UniqueDetectionTool.check(dictData);

        Dict dict = lambdaQuery()
                .eq(Dict::getKey, dictData.getKey())
                .one();
        Assert.notNull(dict, "字典不存在");
        lambdaUpdate()
                .eq(Dict::getKey, dictData.getKey())
                .set(Dict::getVersion, dict.getVersion() + 1)
                .update();

        Assert.isTrue(dictDataService.save(dictData), "保存失败");
    }

    @Override
    @Caching(
            evict = {
                    @CacheEvict(value = "strix:dict:dictResp", key = "#dictData.key"),
                    @CacheEvict(value = "strix:dict:versionMap", allEntries = true)
            }
    )
    @Transactional(rollbackFor = Exception.class)
    public void updateDictData(DictData dictData, DictDataUpdateReq req) {
        UpdateWrapper<DictData> updateWrapper = UpdateConditionBuilder.build(dictData, req, SecurityUtils.getUserId());
        UniqueDetectionTool.check(dictData);


        Dict dict = lambdaQuery()
                .eq(Dict::getKey, dictData.getKey())
                .one();
        Assert.notNull(dict, "字典不存在");
        lambdaUpdate()
                .eq(Dict::getKey, dictData.getKey())
                .set(Dict::getVersion, dict.getVersion() + 1)
                .update();

        Assert.isTrue(dictDataService.update(updateWrapper), "保存失败");
    }

    @Override
    @Caching(
            evict = {
                    @CacheEvict(value = "strix:dict:dictResp", key = "#dictData.key"),
                    @CacheEvict(value = "strix:dict:versionMap", allEntries = true)
            }
    )
    @Transactional(rollbackFor = Exception.class)
    public void removeDictData(DictData dictData) {
        Dict dict = lambdaQuery()
                .eq(Dict::getKey, dictData.getKey())
                .one();
        Assert.notNull(dict, "字典不存在");
        lambdaUpdate()
                .eq(Dict::getKey, dictData.getKey())
                .set(Dict::getVersion, dict.getVersion() + 1)
                .update();

        Assert.isTrue(dictDataService.lambdaUpdate()
                .eq(DictData::getKey, dictData.getKey())
                .eq(DictData::getValue, dictData.getValue())
                .remove(), "删除失败");
    }


}
