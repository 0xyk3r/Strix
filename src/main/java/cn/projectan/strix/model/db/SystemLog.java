package cn.projectan.strix.model.db;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * <p>
 * sys_system_log
 * </p>
 *
 * @author ProjectAn
 * @since 2023-06-16
 */
@Getter
@Setter
@Accessors(chain = true)
@NoArgsConstructor
@AllArgsConstructor
@TableName("sys_system_log")
public class SystemLog implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 应用ID
     */
    @TableId(value = "app_id", type = IdType.INPUT)
    private String appId;

    /**
     * 应用版本
     */
    private String appVersion;

    /**
     * 操作类型
     */
    private String operationType;

    /**
     * 操作分组
     */
    private String operationGroup;

    /**
     * 操作名称
     */
    private String operationName;

    /**
     * 使用时间
     */
    private Long operationSpend;

    /**
     * 操作方法
     */
    private String operationMethod;

    /**
     * 操作URL
     */
    private String operationUrl;

    /**
     * 操作参数
     */
    private String operationParam;

    /**
     * 创建时间
     */
    private LocalDateTime operationTime;

    /**
     * 操作IP
     */
    private String clientIp;

    /**
     * 操作设备
     */
    private String clientDevice;

    /**
     * 操作位置
     */
    private String clientLocation;

    /**
     * 操作用户
     */
    private String clientUser;

    /**
     * 操作用户名称
     */
    private String clientUsername;

    /**
     * 响应状态码
     */
    private Integer responseCode;

    /**
     * 响应消息
     */
    private String responseMsg;

    /**
     * 响应数据
     */
    private String responseData;

}
