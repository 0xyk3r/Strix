package cn.projectan.strix.model.response.system.monitor.log;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author ProjectAn
 */
@Schema(description = "操作日志统计响应")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SystemLogStatsResp {

    @Schema(description = "今日操作总数")
    private Long todayCount;

    @Schema(description = "今日错误数")
    private Long todayErrorCount;

    @Schema(description = "平均响应时间（毫秒）")
    private Long avgResponseTime;

    @Schema(description = "今日活跃用户数")
    private Long activeUserCount;

    @Schema(description = "错误率（百分比）")
    private Double errorRate;

}
