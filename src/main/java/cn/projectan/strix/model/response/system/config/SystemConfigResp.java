package cn.projectan.strix.model.response.system.config;

import cn.projectan.strix.model.db.system.SystemConfig;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Schema(description = "系统配置响应")
@Data
@NoArgsConstructor
public class SystemConfigResp {

    @Schema(description = "配置 ID")
    private String id;

    @Schema(description = "配置标识")
    private String key;

    @Schema(description = "配置名称")
    private String name;

    @Schema(description = "配置类型 1=开关 2=内容")
    private Short type;

    @Schema(description = "配置值")
    private String value;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "创建时间")
    private LocalDateTime createdTime;

    @Schema(description = "更新时间")
    private LocalDateTime updatedTime;

    public SystemConfigResp(SystemConfig config) {
        this.id = config.getId();
        this.key = config.getKey();
        this.name = config.getName();
        this.type = config.getType();
        this.value = config.getValue();
        this.remark = config.getRemark();
        this.createdTime = config.getCreatedTime();
        this.updatedTime = config.getUpdatedTime();
    }
}
