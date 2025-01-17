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
    private LocalDateTime createdTime;

    /**
     * 数据创建人类型
     */
    @TableField(fill = FieldFill.INSERT)
    private Short createdByType;

    /**
     * 数据创建人
     */
    @TableField(fill = FieldFill.INSERT)
    private String createdBy;

    /**
     * 数据修改时间
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedTime;

    /**
     * 数据修改人类型
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Short updatedByType;

    /**
     * 数据修改人
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private String updatedBy;

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
    public T setCreatedTime(LocalDateTime createdTime) {
        this.createdTime = createdTime;
        return (T) this;
    }

    @SuppressWarnings("unchecked")
    public T setCreatedByType(Short createdByType) {
        this.createdByType = createdByType;
        return (T) this;
    }

    @SuppressWarnings("unchecked")
    public T setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
        return (T) this;
    }

    @SuppressWarnings("unchecked")
    public T setUpdatedTime(LocalDateTime updatedTime) {
        this.updatedTime = updatedTime;
        return (T) this;
    }

    @SuppressWarnings("unchecked")
    public T setUpdatedByType(Short updatedByType) {
        this.updatedByType = updatedByType;
        return (T) this;
    }

    @SuppressWarnings("unchecked")
    public T setUpdatedBy(String updatedBy) {
        this.updatedBy = updatedBy;
        return (T) this;
    }
}
