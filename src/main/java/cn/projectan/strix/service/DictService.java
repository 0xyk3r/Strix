package cn.projectan.strix.service;

import cn.projectan.strix.model.db.Dict;
import cn.projectan.strix.model.db.DictData;
import cn.projectan.strix.model.request.system.dict.DictDataUpdateReq;
import cn.projectan.strix.model.request.system.dict.DictUpdateReq;
import cn.projectan.strix.model.response.common.CommonDictResp;
import cn.projectan.strix.model.response.common.CommonDictVersionResp;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * <p>
 * 服务类
 * </p>
 *
 * @author ProjectAn
 * @since 2021-08-31
 */
public interface DictService extends IService<Dict> {

    /**
     * 获取字典版本
     *
     * @return 字典版本
     */
    CommonDictVersionResp getDictVersionMapResp();

    /**
     * 获取字典数据
     *
     * @param key 字典key
     * @return 字典
     */
    CommonDictResp getDictResp(String key);

    /**
     * 保存字典
     *
     * @param dict
     */
    void saveDict(Dict dict);

    /**
     * 更新字典
     *
     * @param dict
     * @param req
     */
    void updateDict(Dict dict, DictUpdateReq req);

    /**
     * 删除字典
     *
     * @param dict
     */
    void removeDict(Dict dict);

    /**
     * 保存字典数据
     *
     * @param dictData
     */
    void saveDictData(DictData dictData);

    /**
     * 更新字典数据
     *
     * @param dictData
     * @param req
     */
    void updateDictData(DictData dictData, DictDataUpdateReq req);

    /**
     * 删除字典数据
     *
     * @param dictData
     */
    void removeDictData(DictData dictData);

}
