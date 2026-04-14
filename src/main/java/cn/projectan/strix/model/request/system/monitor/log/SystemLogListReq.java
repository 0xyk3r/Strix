package cn.projectan.strix.model.request.system.monitor.log;

import cn.projectan.strix.model.db.system.SystemLog;
import cn.projectan.strix.model.request.base.BasePageReq;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

/**
 * @author ProjectAn
 * @since 2023/6/17 22:27
 */
@Schema(description = "系统日志列表请求")
@Data
public class SystemLogListReq extends BasePageReq<SystemLog> {

    /**
     * 操作名称
     */
    @Schema(description = "操作名称")
    @Size(max = 64)
    private String keyword;

    /**
     * 操作类型
     */
    @Schema(description = "操作类型")
    private String operationType;

    /**
     * 操作分组
     */
    @Schema(description = "操作分组")
    private String operationGroup;

    /**
     * 响应状态码
     */
    @Schema(description = "响应状态码")
    private Integer responseCode;

    /**
     * 操作开始时间
     */
    @Schema(description = "操作开始时间")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime startTime;

    /**
     * 操作结束时间
     */
    @Schema(description = "操作结束时间")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime endTime;

    /**
     * 操作用户名
     */
    @Schema(description = "操作用户名")
    @Size(max = 64)
    private String clientUsername;

    /**
     * 客户端IP
     */
    @Schema(description = "客户端IP")
    @Size(max = 64)
    private String clientIp;

}
