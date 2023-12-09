package cn.projectan.strix.model.db;

import cn.projectan.strix.model.db.base.BaseModel;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.io.Serial;
import java.time.LocalDateTime;

/**
 * <p>
 *
 * </p>
 *
 * @author 安炯奕
 * @since 2023-11-29
 */
@Getter
@Setter
@Accessors(chain = true)
@NoArgsConstructor
@AllArgsConstructor
@TableName("sys_workflow_invoke")
public class WorkflowInvoke extends BaseModel {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 完整调用字符串
     */
    private String fullInvokeStr;

    /**
     * 调用Bean
     */
    private String invokeBean;

    /**
     * 调用方法
     */
    private String invokeMethod;

    /**
     * 是否系统内置 0否 1是
     */
    private Byte builtinFlag;

    public WorkflowInvoke(String createBy, String updateBy) {
        super(createBy, updateBy);
    }

    public WorkflowInvoke(LocalDateTime createTime, String createBy, LocalDateTime updateTime, String updateBy) {
        super(createTime, createBy, updateTime, updateBy);
    }

}
