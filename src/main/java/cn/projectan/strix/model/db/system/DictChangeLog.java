package cn.projectan.strix.model.db.system;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

/**
 * 字典变更历史
 * <p>
 * 不继承 BaseModel，无软删除。超过保留期可通过定时任务清理。
 *
 * @author ProjectAn
 * @since 2026-04-19
 */
@Getter
@Setter
@Accessors(chain = true)
@NoArgsConstructor
@TableName("sys_dict_change_log")
public class DictChangeLog {

    @TableId(type = IdType.ASSIGN_ID)
    private String id;

    /** 字典 key */
    private String dictKey;

    /** 变更类型 */
    private String changeType;

    /** 变更前快照 (JSON) */
    private String snapshotBefore;

    /** 变更后快照 (JSON) */
    private String snapshotAfter;

    /** 操作人 ID */
    private String operatorId;

    /** 操作人昵称 */
    private String operatorName;

    /** 备注 */
    private String remark;

    /** 创建时间 */
    private LocalDateTime createdTime;

}
