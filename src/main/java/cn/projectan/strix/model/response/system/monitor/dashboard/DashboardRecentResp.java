package cn.projectan.strix.model.response.system.monitor.dashboard;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Schema(description = "最近操作数据响应")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DashboardRecentResp {

    @Schema(description = "最近操作列表")
    private List<DashboardRecentItem> items;
}
