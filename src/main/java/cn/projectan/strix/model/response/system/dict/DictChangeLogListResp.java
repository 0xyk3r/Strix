package cn.projectan.strix.model.response.system.dict;

import cn.projectan.strix.model.db.system.DictChangeLog;
import cn.projectan.strix.model.response.base.BasePageResp;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author ProjectAn
 * @since 2026-04-19
 */
@Schema(description = "字典变更历史列表响应")
@Getter
@NoArgsConstructor
public class DictChangeLogListResp extends BasePageResp {

    @Schema(description = "变更记录列表")
    private List<ChangeLogItem> items = new ArrayList<>();

    public DictChangeLogListResp(List<DictChangeLog> data, long total) {
        items = data.stream().map(log -> new ChangeLogItem(
                log.getId(), log.getDictKey(), log.getChangeType(),
                log.getSnapshotBefore(), log.getSnapshotAfter(),
                log.getOperatorId(), log.getOperatorName(),
                log.getRemark(), log.getCreatedTime()
        )).collect(Collectors.toList());
        this.setTotal(total);
    }

    @Schema(description = "变更记录项")
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ChangeLogItem {
        @Schema(description = "记录 ID")
        private String id;
        @Schema(description = "字典 Key")
        private String dictKey;
        @Schema(description = "变更类型")
        private String changeType;
        @Schema(description = "变更前快照")
        private String snapshotBefore;
        @Schema(description = "变更后快照")
        private String snapshotAfter;
        @Schema(description = "操作人 ID")
        private String operatorId;
        @Schema(description = "操作人昵称")
        private String operatorName;
        @Schema(description = "备注")
        private String remark;
        @Schema(description = "变更时间")
        private LocalDateTime createdTime;
    }

}
