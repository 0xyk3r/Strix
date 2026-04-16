package cn.projectan.strix.model.response.system.monitor.dashboard;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Schema(description = "日趋势数据响应")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DashboardTrendsResp {

    @Schema(description = "趋势数据列表")
    private List<DashboardTrendItem> items;
}
