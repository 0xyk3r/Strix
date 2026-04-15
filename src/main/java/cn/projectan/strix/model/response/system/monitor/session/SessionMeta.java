package cn.projectan.strix.model.response.system.monitor.session;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 会话元数据 — 存储在 Redis Hash 值中 (JSON 序列化)
 *
 * @author ProjectAn
 */
@Schema(description = "会话元数据")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SessionMeta {

    @Schema(description = "登录时间")
    private LocalDateTime loginTime;

    @Schema(description = "客户端 IP")
    private String ip;

    @Schema(description = "设备/操作系统")
    private String device;

    @Schema(description = "User-Agent 原始值")
    private String userAgent;

    @Schema(description = "最后活跃时间")
    private LocalDateTime lastActiveTime;
}
