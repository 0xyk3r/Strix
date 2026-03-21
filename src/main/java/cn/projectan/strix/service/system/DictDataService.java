package cn.projectan.strix.service.system;

import cn.projectan.strix.mapper.system.DictDataMapper;
import cn.projectan.strix.model.db.system.DictData;
import cn.projectan.strix.model.enums.common.NumCategory;
import cn.projectan.strix.model.request.system.dict.DictDataListReq;
import cn.projectan.strix.util.math.NumUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * <p>
 * Strix 字典数据 服务类
 * </p>
 *
 * @author ProjectAn
 * @since 2023-05-28
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DictDataService extends ServiceImpl<DictDataMapper, DictData> {

    /**
     * 根据字典key查询字典数据列表
     *
     * @param key 字典key
     * @return 字典数据列表
     */
    public List<DictData> listByKey(String key) {
        return lambdaQuery()
                .eq(DictData::getKey, key)
                .list();
    }

    /**
     * 分页查询字典数据列表
     *
     * @param key 字典key
     * @param req 查询请求
     * @return 分页结果
     */
    public Page<DictData> listPage(String key, DictDataListReq req) {
        return lambdaQuery()
                .eq(DictData::getKey, key)
                .like(StringUtils.hasText(req.getKeyword()), DictData::getValue, req.getKeyword())
                .or(StringUtils.hasText(req.getKeyword()), q -> q.like(DictData::getLabel, req.getKeyword()))
                .eq(NumUtil.checkCategory(req.getStatus(), NumCategory.NON_NEGATIVE), DictData::getStatus, req.getStatus())
                .orderByAsc(DictData::getSort)
                .page(req.getPage());
    }

}
