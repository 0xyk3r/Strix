package cn.projectan.strix.controller.system.monitor;

import cn.projectan.strix.controller.system.base.BaseSystemController;
import cn.projectan.strix.core.ret.RetBuilder;
import cn.projectan.strix.core.ret.RetResult;
import cn.projectan.strix.model.db.system.SystemAnnouncement;
import cn.projectan.strix.model.request.base.BasePageReq;
import cn.projectan.strix.model.request.system.announcement.PublishAnnouncementReq;
import cn.projectan.strix.model.response.system.announcement.AnnouncementListResp;
import cn.projectan.strix.service.system.SystemAnnouncementService;
import cn.projectan.strix.util.system.SecurityUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 系统公告管理
 *
 * @author ProjectAn
 * @since 2026-04-18
 */
@Slf4j
@RestController
@RequestMapping("system/monitor/announcement")
@RequiredArgsConstructor
@Tag(name = "系统监控 - 系统公告")
public class SystemAnnouncementController extends BaseSystemController {

    private final SystemAnnouncementService systemAnnouncementService;

    @GetMapping("")
    @PreAuthorize("@ss.hasPermission('system:monitor:announcement')")
    @Operation(summary = "公告管理列表")
    public RetResult<AnnouncementListResp> list(
            BasePageReq<SystemAnnouncement> pageReq,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Short status,
            @RequestParam(required = false) String level) {
        return RetBuilder.success(systemAnnouncementService.getManageList(pageReq, keyword, status, level));
    }

    @GetMapping("{id}")
    @PreAuthorize("@ss.hasPermission('system:monitor:announcement')")
    @Operation(summary = "公告详情")
    public RetResult<SystemAnnouncement> detail(@PathVariable String id) {
        return RetBuilder.success(systemAnnouncementService.getDetail(id));
    }

    @PostMapping("publish")
    @PreAuthorize("@ss.hasPermission('system:monitor:announcement:publish')")
    @Operation(summary = "发布公告")
    public RetResult<Object> publish(@RequestBody @Valid PublishAnnouncementReq req) {
        systemAnnouncementService.publish(req);
        return RetBuilder.success();
    }

    @PostMapping("{id}/terminate")
    @PreAuthorize("@ss.hasPermission('system:monitor:announcement:terminate')")
    @Operation(summary = "终止公告")
    public RetResult<Object> terminate(@PathVariable String id, @RequestBody(required = false) TerminateReq req) {
        String operatorId = SecurityUtil.getOperatorId();
        String reason = req != null ? req.reason() : "管理员手动终止";
        systemAnnouncementService.terminate(id, operatorId, reason);
        return RetBuilder.success();
    }

    @PostMapping("batch-terminate")
    @PreAuthorize("@ss.hasPermission('system:monitor:announcement:terminate')")
    @Operation(summary = "批量终止公告")
    public RetResult<Object> batchTerminate(@RequestBody BatchTerminateReq req) {
        String operatorId = SecurityUtil.getOperatorId();
        for (String id : req.ids()) {
            systemAnnouncementService.terminate(id, operatorId, "批量终止");
        }
        return RetBuilder.success();
    }

    record TerminateReq(String reason) {}
    record BatchTerminateReq(List<String> ids) {}
}
