package cn.projectan.strix.model.db.base;

import com.baomidou.mybatisplus.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * @author ProjectAn
 * @since 2021/5/2 16:54
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class BaseModel<T extends BaseModel<T>> implements java.io.Serializable {

    /**
     * 主键
     */
    @TableId(type = IdType.ASSIGN_ID)
    private String id;

    /**
     * 数据删除状态 0正常 1删除
     */
    @TableLogic
    @TableField(fill = FieldFill.INSERT)
    private Integer deletedStatus;

    /**
     * 数据创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /**
     * 数据创建人
     */
    @TableField(fill = FieldFill.INSERT)
    private String createBy;

    /**
     * 数据修改时间
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    /**
     * 数据修改人
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private String updateBy;

    @SuppressWarnings("unchecked")
    public T setId(String id) {
        this.id = id;
        return (T) this;
    }

    @SuppressWarnings("unchecked")
    public T setDeletedStatus(Integer deletedStatus) {
        this.deletedStatus = deletedStatus;
        return (T) this;
    }

    @SuppressWarnings("unchecked")
    public T setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
        return (T) this;
    }

    @SuppressWarnings("unchecked")
    public T setCreateBy(String createBy) {
        this.createBy = createBy;
        return (T) this;
    }

    @SuppressWarnings("unchecked")
    public T setUpdateTime(LocalDateTime updateTime) {
        this.updateTime = updateTime;
        return (T) this;
    }

    @SuppressWarnings("unchecked")
    public T setUpdateBy(String updateBy) {
        this.updateBy = updateBy;
        return (T) this;
    }

}
