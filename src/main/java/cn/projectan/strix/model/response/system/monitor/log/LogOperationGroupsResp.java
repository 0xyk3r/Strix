package cn.projectan.strix.model.response.system.monitor.log;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Schema(description = "操作分组列表响应")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LogOperationGroupsResp {

    @Schema(description = "操作分组名称列表")
    private List<String> items;
}
