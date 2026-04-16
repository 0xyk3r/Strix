package cn.projectan.strix.model.response.system.config;

import cn.projectan.strix.model.db.system.SystemConfig;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Schema(description = "系统配置列表响应")
@Data
@NoArgsConstructor
public class SystemConfigListResp {

    @Schema(description = "配置列表")
    private List<SystemConfigResp> items;

    public SystemConfigListResp(List<SystemConfig> configs) {
        this.items = configs.stream().map(SystemConfigResp::new).toList();
    }
}
