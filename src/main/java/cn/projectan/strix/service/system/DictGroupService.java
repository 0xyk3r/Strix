package cn.projectan.strix.service.system;

import cn.projectan.strix.mapper.system.DictGroupMapper;
import cn.projectan.strix.model.db.system.DictGroup;
import cn.projectan.strix.model.request.system.dict.DictGroupUpdateReq;
import cn.projectan.strix.util.common.I18nUtil;
import cn.projectan.strix.util.common.UniqueChecker;
import cn.projectan.strix.util.common.UpdateBuilder;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import java.util.List;

/**
 * 字典分组服务
 *
 * @author ProjectAn
 * @since 2026-04-19
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DictGroupService extends ServiceImpl<DictGroupMapper, DictGroup> {

    /**
     * 获取所有分组（按排序值排序）
     */
    public List<DictGroup> listAll() {
        return lambdaQuery()
                .orderByAsc(DictGroup::getSortValue)
                .orderByAsc(DictGroup::getCreatedTime)
                .list();
    }

    /**
     * 新增分组
     */
    public void saveGroup(DictGroupUpdateReq req) {
        DictGroup group = new DictGroup()
                .setName(req.getName())
                .setIcon(req.getIcon())
                .setSortValue(req.getSortValue());
        UniqueChecker.check(group);
        Assert.isTrue(save(group), "保存失败");
    }

    /**
     * 修改分组
     */
    public void updateGroup(String id, DictGroupUpdateReq req) {
        DictGroup group = getById(id);
        Assert.notNull(group, I18nUtil.notFound("field.dictGroup"));
        LambdaUpdateWrapper<DictGroup> wrapper = UpdateBuilder.build(group, req);
        UniqueChecker.check(group);
        Assert.isTrue(update(wrapper), "保存失败");
    }

    /**
     * 删除分组
     * 调用前需由 controller 检查是否有字典引用此分组
     */
    public void deleteGroup(String id) {
        DictGroup group = getById(id);
        Assert.notNull(group, I18nUtil.notFound("field.dictGroup"));
        Assert.isTrue(removeById(id), "删除失败");
    }

}
