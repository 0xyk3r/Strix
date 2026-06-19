package cn.projectan.strix.service.system;

import cn.projectan.strix.mapper.system.DictChangeLogMapper;
import cn.projectan.strix.model.db.system.DictChangeLog;
import cn.projectan.strix.model.db.system.DictData;
import cn.projectan.strix.model.request.base.BasePageReq;
import cn.projectan.strix.util.common.ObjectMapperUtil;
import cn.projectan.strix.util.system.SecurityUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 字典变更历史服务
 *
 * @author ProjectAn
 * @since 2026-04-19
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DictChangeLogService extends ServiceImpl<DictChangeLogMapper, DictChangeLog> {

    /**
     * 记录变更
     *
     * @param dictKey    字典 key
     * @param changeType 变更类型 (see DictChangeType constants)
     * @param before     变更前数据 (可为 null)
     * @param after      变更后数据 (可为 null)
     * @param remark     备注
     */
    public void record(String dictKey, String changeType, Object before, Object after, String remark) {
        try {
            String operatorId = null;
            String operatorName = null;
            try {
                operatorId = SecurityUtil.getOperatorId();
                operatorName = SecurityUtil.getSystemManager().getNickname();
            } catch (Exception e) {
                // 启动时同步或系统任务等场景无 SecurityContext
                log.debug("获取当前操作人信息失败（可能为系统任务）: {}", e.getMessage());
            }

            DictChangeLog logEntry = new DictChangeLog()
                    .setDictKey(dictKey)
                    .setChangeType(changeType)
                    .setSnapshotBefore(before != null ? ObjectMapperUtil.get().writeValueAsString(before) : null)
                    .setSnapshotAfter(after != null ? ObjectMapperUtil.get().writeValueAsString(after) : null)
                    .setOperatorId(operatorId)
                    .setOperatorName(operatorName)
                    .setRemark(remark)
                    .setCreatedTime(LocalDateTime.now());
            save(logEntry);
        } catch (Exception e) {
            log.warn("记录字典变更历史失败: dictKey={}, changeType={}", dictKey, changeType, e);
        }
    }

    /**
     * 按字典 key 分页查询变更历史
     */
    public Page<DictChangeLog> listByDictKey(String dictKey, BasePageReq<DictChangeLog> pageReq) {
        return lambdaQuery()
                .eq(DictChangeLog::getDictKey, dictKey)
                .orderByDesc(DictChangeLog::getCreatedTime)
                .page(pageReq.getPage());
    }

    /**
     * 获取单条变更记录（用于回滚）
     */
    public DictChangeLog getLog(String logId) {
        return getById(logId);
    }

    /**
     * 从快照 JSON 反序列化 DictData 列表
     * 兼容历史记录中的单个对象格式 (非数组)
     */
    public List<DictData> deserializeSnapshot(String snapshotJson) {
        try {
            String trimmed = snapshotJson.trim();
            if (trimmed.startsWith("[")) {
                return ObjectMapperUtil.get().readValue(snapshotJson,
                        ObjectMapperUtil.get().getTypeFactory().constructCollectionType(List.class, DictData.class));
            } else {
                DictData single = ObjectMapperUtil.get().readValue(snapshotJson, DictData.class);
                return List.of(single);
            }
        } catch (Exception e) {
            throw new IllegalArgumentException("反序列化快照数据失败", e);
        }
    }

}
