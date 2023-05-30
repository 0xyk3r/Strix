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
 * @author 安炯奕
 * @since 2021-08-31
 */
public interface DictService extends IService<Dict> {

    CommonDictVersionResp getDictVersionMapResp();

    CommonDictResp getDictResp(String key);

    void saveDict(Dict dict);

    void updateDict(Dict dict, DictUpdateReq req);

    void removeDict(Dict dict);

    void saveDictData(DictData dictData);

    void updateDictData(DictData dictData, DictDataUpdateReq req);

    void removeDictData(DictData dictData);

}
