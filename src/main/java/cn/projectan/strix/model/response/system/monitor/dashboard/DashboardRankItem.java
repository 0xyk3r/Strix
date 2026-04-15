package cn.projectan.strix.model.response.system.monitor.dashboard;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Schema(description = "仪表板排名数据项")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DashboardRankItem {

    @Schema(description = "名称 (用户名/模块名/操作名)")
    private String name;

    @Schema(description = "数量")
    private Long count;
}
