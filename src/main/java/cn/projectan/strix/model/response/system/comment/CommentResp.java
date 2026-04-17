package cn.projectan.strix.model.response.system.comment;

import cn.projectan.strix.model.db.system.Comment;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 评论响应
 *
 * @author ProjectAn
 */
@Schema(description = "评论响应")
@Data
@NoArgsConstructor
public class CommentResp {

    @Schema(description = "评论 ID")
    private String id;

    @Schema(description = "业务类型")
    private String bizType;

    @Schema(description = "业务主键 ID")
    private String bizId;

    @Schema(description = "评论内容")
    private String content;

    @Schema(description = "@提及的管理员 ID 列表")
    private List<String> mentionedIds;

    @Schema(description = "附件文件 ID 列表")
    private List<String> attachmentIds;

    @Schema(description = "是否置顶")
    private Short pinned;

    @Schema(description = "作者 ID")
    private String createdBy;

    @Schema(description = "作者类型")
    private Short createdByType;

    @Schema(description = "作者昵称")
    private String authorName;

    @Schema(description = "创建时间")
    private LocalDateTime createdTime;

    @Schema(description = "更新时间")
    private LocalDateTime updatedTime;

    @Schema(description = "是否为当前用户的评论")
    private boolean mine;

    @Schema(description = "是否可编辑（本人且在5分钟内）")
    private boolean editable;

    @Schema(description = "Emoji 反应统计: emoji -> [{operatorId, operatorName}]")
    private Map<String, List<ReactionUser>> reactions;

    @Schema(description = "反应用户")
    @Data
    @NoArgsConstructor
    public static class ReactionUser {
        @Schema(description = "操作人 ID")
        private String operatorId;
        @Schema(description = "操作人昵称")
        private String operatorName;

        public ReactionUser(String operatorId, String operatorName) {
            this.operatorId = operatorId;
            this.operatorName = operatorName;
        }
    }

    public CommentResp(Comment comment) {
        this.id = comment.getId();
        this.bizType = comment.getBizType();
        this.bizId = comment.getBizId();
        this.content = comment.getContent();
        this.mentionedIds = comment.getMentionedIds();
        this.attachmentIds = comment.getAttachmentIds();
        this.pinned = comment.getPinned();
        this.createdBy = comment.getCreatedBy();
        this.createdByType = comment.getCreatedByType();
        this.createdTime = comment.getCreatedTime();
        this.updatedTime = comment.getUpdatedTime();
    }

}
