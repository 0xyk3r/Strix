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
 * 字典使用统计（静态扫描）
 * <p>
 * 不继承 BaseModel，无软删除。每次应用启动时全量刷新。
 *
 * @author ProjectAn
 * @since 2026-04-19
 */
@Getter
@Setter
@Accessors(chain = true)
@NoArgsConstructor
@TableName("sys_dict_usage_stat")
public class DictUsageStat {

    @TableId(type = IdType.ASSIGN_ID)
    private String id;

    /** 字典 key */
    private String dictKey;

    /** 使用类型: VALIDATION / FRONTEND / CONSTANT */
    private String usageType;

    /** 使用位置（类名.字段名 或 文件路径） */
    private String usageLocation;

    /** 扫描时间 */
    private LocalDateTime scannedAt;

}
