package cn.projectan.strix.model.db.base;

import com.baomidou.mybatisplus.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * @author 安炯奕
 * @date 2021/5/2 16:54
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BaseModel implements java.io.Serializable {

    /**
     * 主键
     */
    @TableId(type = IdType.ASSIGN_ID)
    private String id;

    /**
     * 数据删除状态 0正常 1删除
     */
    @TableLogic
    private Integer deletedStatus;

    /**
     * 数据创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /**
     * 数据创建人
     */
    private String createBy;

    /**
     * 数据修改时间
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    /**
     * 数据修改人
     */
    private String updateBy;

    public BaseModel(String createBy, String updateBy) {
        this.createBy = createBy;
        this.updateBy = updateBy;
    }

    public BaseModel(LocalDateTime createTime, String createBy, LocalDateTime updateTime, String updateBy) {
        this.createTime = createTime;
        this.createBy = createBy;
        this.updateTime = updateTime;
        this.updateBy = updateBy;
    }
}
