package cn.projectan.strix.controller.system;

import cn.projectan.strix.controller.system.base.BaseSystemController;
import cn.projectan.strix.core.ret.RetBuilder;
import cn.projectan.strix.core.ret.RetResult;
import cn.projectan.strix.model.annotation.StrixLog;
import cn.projectan.strix.model.db.system.Comment;
import cn.projectan.strix.model.dict.system.SystemLogOperType;
import cn.projectan.strix.model.request.system.comment.*;
import cn.projectan.strix.model.response.system.comment.CommentBatchCountResp;
import cn.projectan.strix.model.response.system.comment.CommentListResp;
import cn.projectan.strix.service.system.CommentReactionService;
import cn.projectan.strix.service.system.CommentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * 通用评论管理
 *
 * @author ProjectAn
 */
@Slf4j
@RestController
@RequestMapping("system/comment")
@RequiredArgsConstructor
@Tag(name = "System - 通用评论")
public class CommentController extends BaseSystemController {

    private final CommentService commentService;
    private final CommentReactionService commentReactionService;

    @Operation(summary = "评论列表")
    @GetMapping("")
    @PreAuthorize("@ss.hasPermission('system:comment')")
    public RetResult<CommentListResp> list(@Valid CommentListReq req) {
        return RetBuilder.success(commentService.list(req));
    }

    @Operation(summary = "新增评论")
    @PostMapping("")
    @PreAuthorize("@ss.hasPermission('system:comment:add')")
    @StrixLog(operationGroup = "通用评论", operationName = "新增评论", operationType = SystemLogOperType.ADD)
    public RetResult<Object> add(@RequestBody @Valid CommentAddReq req) {
        Comment comment = commentService.add(req);
        return RetBuilder.success(comment.getId());
    }

    @Operation(summary = "编辑评论")
    @PostMapping("update/{id}")
    @PreAuthorize("@ss.hasPermission('system:comment:add')")
    @StrixLog(operationGroup = "通用评论", operationName = "编辑评论", operationType = SystemLogOperType.UPDATE)
    public RetResult<Void> update(
            @Parameter(description = "评论 ID") @PathVariable String id,
            @RequestBody @Valid CommentUpdateReq req) {
        commentService.update(id, req);
        return RetBuilder.success();
    }

    @Operation(summary = "删除评论")
    @PostMapping("remove/{id}")
    @PreAuthorize("@ss.hasPermission('system:comment:add')")
    @StrixLog(operationGroup = "通用评论", operationName = "删除评论", operationType = SystemLogOperType.DELETE)
    public RetResult<Void> remove(@Parameter(description = "评论 ID") @PathVariable String id) {
        boolean hasDeletePermission = isSuperManager();
        commentService.delete(id, hasDeletePermission);
        return RetBuilder.success();
    }

    @Operation(summary = "切换置顶")
    @PostMapping("{id}/pin")
    @PreAuthorize("@ss.hasPermission('system:comment:pin')")
    @StrixLog(operationGroup = "通用评论", operationName = "切换评论置顶", operationType = SystemLogOperType.UPDATE)
    public RetResult<Void> togglePin(@Parameter(description = "评论 ID") @PathVariable String id) {
        commentService.togglePin(id);
        return RetBuilder.success();
    }

    @Operation(summary = "批量获取评论数")
    @PostMapping("count/batch")
    @PreAuthorize("@ss.hasPermission('system:comment')")
    public RetResult<CommentBatchCountResp> batchCount(@RequestBody @Valid CommentBatchCountReq req) {
        return RetBuilder.success(new CommentBatchCountResp(
                commentService.batchCount(req.getBizType(), req.getBizIds())));
    }

    @Operation(summary = "切换 Emoji 反应")
    @PostMapping("{id}/reaction")
    @PreAuthorize("@ss.hasPermission('system:comment:add')")
    public RetResult<Object> toggleReaction(
            @Parameter(description = "评论 ID") @PathVariable String id,
            @RequestBody @Valid CommentReactionReq req) {
        boolean added = commentReactionService.toggle(id, req.getEmoji(), loginManagerId());
        return RetBuilder.success(added);
    }

}
