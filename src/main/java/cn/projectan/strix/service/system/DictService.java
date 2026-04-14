package cn.projectan.strix.service.system;

import cn.projectan.strix.mapper.system.DictMapper;
import cn.projectan.strix.model.db.system.Dict;
import cn.projectan.strix.model.db.system.DictData;
import cn.projectan.strix.model.dict.common.CommonSwitch;
import cn.projectan.strix.model.enums.common.NumCategory;
import cn.projectan.strix.model.request.system.dict.DictDataUpdateReq;
import cn.projectan.strix.model.request.system.dict.DictListReq;
import cn.projectan.strix.model.request.system.dict.DictUpdateReq;
import cn.projectan.strix.model.response.common.CommonDictResp;
import cn.projectan.strix.model.response.common.CommonDictVersionResp;
import cn.projectan.strix.model.response.system.dict.DictDataListResp;
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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

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

        List<DictData> dictDataList = dictDataService.lambdaQuery()
                .eq(DictData::getKey, key)
                .eq(DictData::getStatus, CommonSwitch.ENABLE)
                .orderByAsc(DictData::getSort)
                .list();

        if (dict == null || CollectionUtils.isEmpty(dictDataList)) {
            return null;
        }

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
        Assert.isTrue(dictDataService.lambdaUpdate()
                .eq(DictData::getKey, dict.getKey())
                .remove(), "删除失败");
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
        incrementDictVersion(dictData.getKey());
        Assert.isTrue(dictDataService.save(dictData), "保存失败");
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
        LambdaUpdateWrapper<DictData> updateWrapper = UpdateBuilder.build(dictData, req);
        UniqueChecker.check(dictData);
        incrementDictVersion(dictData.getKey());
        Assert.isTrue(dictDataService.update(updateWrapper), "保存失败");
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
        incrementDictVersion(dictData.getKey());

        dictDataService.lambdaUpdate()
                .eq(DictData::getKey, dictData.getKey())
                .eq(DictData::getValue, dictData.getValue())
                .remove();
    }

    private void incrementDictVersion(String dictKey) {
        boolean updated = lambdaUpdate()
                .eq(Dict::getKey, dictKey)
                .setSql("version = version + 1")
                .update();
        Assert.isTrue(updated, I18nUtil.notFound("field.dict"));
    }


}
