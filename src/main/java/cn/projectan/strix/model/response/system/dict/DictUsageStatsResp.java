package cn.projectan.strix.model.response.system.dict;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * @author ProjectAn
 * @since 2026-04-19
 */
@Schema(description = "字典使用统计响应")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DictUsageStatsResp {

    @Schema(description = "静态引用列表")
    private List<UsageItem> staticUsages;

    @Schema(description = "运行时访问次数（来自 Redis）")
    private long accessCount;

    @Schema(description = "使用统计项")
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UsageItem {
        @Schema(description = "使用类型: VALIDATION / FRONTEND / CONSTANT")
        private String usageType;
        @Schema(description = "使用位置")
        private String usageLocation;
    }

}
