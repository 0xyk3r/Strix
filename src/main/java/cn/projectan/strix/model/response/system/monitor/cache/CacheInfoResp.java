package cn.projectan.strix.model.response.system.monitor.cache;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;
import java.util.Properties;

@Schema(description = "缓存信息响应")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CacheInfoResp {

    @Schema(description = "Redis 服务器信息")
    private Properties info;

    @Schema(description = "数据库大小 (key 数量)")
    private Object dbSize;

    @Schema(description = "命令统计 (name + value)")
    private List<Map<String, String>> commandStats;
}
