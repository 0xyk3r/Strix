package cn.projectan.strix.model.response.system.monitor.dashboard;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Schema(description = "仪表板小时趋势数据项")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DashboardHourlyItem {

    @Schema(description = "小时 (0-23)")
    private Integer hour;

    @Schema(description = "操作数量")
    private Long count;
}
