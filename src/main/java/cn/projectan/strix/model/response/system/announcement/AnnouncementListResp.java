package cn.projectan.strix.model.response.system.announcement;

import cn.projectan.strix.model.db.system.SystemAnnouncement;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 公告管理列表响应
 *
 * @author ProjectAn
 * @since 2026-04-18
 */
@Schema(description = "公告管理列表响应")
@Data
@NoArgsConstructor
public class AnnouncementListResp {

    @Schema(description = "公告列表")
    private List<AnnouncementItem> items;

    @Schema(description = "分页总数")
    private long total;

    @Schema(description = "总公告数")
    private long totalCount;

    @Schema(description = "活跃公告数")
    private long activeCount;

    @Schema(description = "已终止公告数")
    private long terminatedCount;

    @Schema(description = "公告列表项")
    @Data
    @NoArgsConstructor
    public static class AnnouncementItem {

        @Schema(description = "公告 ID")
        private String id;

        @Schema(description = "公告标题")
        private String title;

        @Schema(description = "级别")
        private String level;

        @Schema(description = "展示方式")
        private String displayType;

        @Schema(description = "状态: 1=有效, 0=已终止")
        private Short status;

        @Schema(description = "生效时间")
        private LocalDateTime startTime;

        @Schema(description = "失效时间")
        private LocalDateTime endTime;

        @Schema(description = "创建时间")
        private LocalDateTime createdTime;

        @Schema(description = "终止原因")
        private String endReason;

        public AnnouncementItem(SystemAnnouncement a) {
            this.id = a.getId();
            this.title = a.getTitle();
            this.level = a.getLevel();
            this.displayType = a.getDisplayType();
            this.status = a.getStatus();
            this.startTime = a.getStartTime();
            this.endTime = a.getEndTime();
            this.createdTime = a.getCreatedTime();
            this.endReason = a.getEndReason();
        }
    }
}
