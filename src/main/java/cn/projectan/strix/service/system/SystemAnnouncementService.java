package cn.projectan.strix.service.system;

import cn.projectan.strix.core.sse.SseSessionManager;
import cn.projectan.strix.mapper.system.SystemAnnouncementMapper;
import cn.projectan.strix.model.db.system.SystemAnnouncement;
import cn.projectan.strix.model.dict.common.CommonFlag;
import cn.projectan.strix.model.request.base.BasePageReq;
import cn.projectan.strix.model.request.system.announcement.PublishAnnouncementReq;
import cn.projectan.strix.model.response.system.announcement.AnnouncementListResp;
import cn.projectan.strix.model.response.system.announcement.AnnouncementListResp.AnnouncementItem;
import cn.projectan.strix.util.common.I18nUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 系统公告服务
 *
 * @author ProjectAn
 * @since 2026-04-18
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SystemAnnouncementService extends ServiceImpl<SystemAnnouncementMapper, SystemAnnouncement> {

    private final SseSessionManager sseSessionManager;

    /**
     * 发布公告
     */
    public void publish(PublishAnnouncementReq req) {
        SystemAnnouncement announcement = new SystemAnnouncement()
                .setTitle(req.getTitle())
                .setContent(req.getContent())
                .setLevel(req.getLevel())
                .setDisplayType(req.getDisplayType())
                .setStatus(CommonFlag.YES)
                .setStartTime(req.getStartTime())
                .setEndTime(req.getEndTime());

        Assert.isTrue(save(announcement), "发布公告失败");
        log.info("公告已发布: id={}, title={}, level={}", announcement.getId(), announcement.getTitle(), announcement.getLevel());

        // 仅当公告当前有效时才立即推送
        if (isCurrentlyActive(announcement)) {
            sseSessionManager.broadcast("system:announce", buildAnnouncementData(announcement));
        }
    }

    /**
     * 终止公告
     */
    public void terminate(String id, String operatorId, String reason) {
        SystemAnnouncement announcement = getById(id);
        Assert.notNull(announcement, I18nUtil.notFound("field.announcement"));

        announcement.setStatus(CommonFlag.NO)
                .setEndBy(operatorId)
                .setEndReason(reason);
        Assert.isTrue(updateById(announcement), "终止公告失败");

        // SSE 广播公告终止
        sseSessionManager.broadcast("system:announce:dismiss", Map.of("id", id));
        log.info("公告已终止: id={}, reason={}", id, reason);
    }

    /**
     * 获取当前所有活跃公告 (供 SSE 连接时初始推送)
     */
    public List<SystemAnnouncement> getActiveAnnouncements() {
        LocalDateTime now = LocalDateTime.now();
        return lambdaQuery()
                .eq(SystemAnnouncement::getStatus, CommonFlag.YES)
                .and(w -> w
                        .isNull(SystemAnnouncement::getStartTime)
                        .or()
                        .le(SystemAnnouncement::getStartTime, now))
                .and(w -> w
                        .isNull(SystemAnnouncement::getEndTime)
                        .or()
                        .gt(SystemAnnouncement::getEndTime, now))
                .orderByDesc(SystemAnnouncement::getCreatedTime)
                .list();
    }

    /**
     * 获取公告管理列表
     */
    public AnnouncementListResp getManageList(BasePageReq<SystemAnnouncement> req, String keyword, Short status, String level) {
        Page<SystemAnnouncement> page = lambdaQuery()
                .like(StringUtils.hasText(keyword), SystemAnnouncement::getTitle, keyword)
                .eq(status != null, SystemAnnouncement::getStatus, status)
                .eq(StringUtils.hasText(level), SystemAnnouncement::getLevel, level)
                .orderByDesc(SystemAnnouncement::getCreatedTime)
                .page(req.getPage());

        List<AnnouncementItem> items = page.getRecords().stream()
                .map(AnnouncementItem::new)
                .toList();

        long totalCount = count();
        long activeCount = lambdaQuery().eq(SystemAnnouncement::getStatus, CommonFlag.YES).count();

        AnnouncementListResp resp = new AnnouncementListResp();
        resp.setItems(items);
        resp.setTotal(page.getTotal());
        resp.setTotalCount(totalCount);
        resp.setActiveCount(activeCount);
        resp.setTerminatedCount(totalCount - activeCount);
        return resp;
    }

    /**
     * 获取公告详情
     */
    public SystemAnnouncement getDetail(String id) {
        SystemAnnouncement announcement = getById(id);
        Assert.notNull(announcement, I18nUtil.notFound("field.announcement"));
        return announcement;
    }

    private boolean isCurrentlyActive(SystemAnnouncement a) {
        LocalDateTime now = LocalDateTime.now();
        boolean started = a.getStartTime() == null || !a.getStartTime().isAfter(now);
        boolean notExpired = a.getEndTime() == null || a.getEndTime().isAfter(now);
        return a.getStatus() == CommonFlag.YES && started && notExpired;
    }

    private Map<String, Object> buildAnnouncementData(SystemAnnouncement a) {
        Map<String, Object> data = new HashMap<>();
        data.put("id", a.getId());
        data.put("title", a.getTitle());
        data.put("content", Optional.ofNullable(a.getContent()).orElse(""));
        data.put("level", a.getLevel());
        data.put("displayType", a.getDisplayType());
        data.put("startTime", a.getStartTime() != null ? a.getStartTime().toString() : null);
        data.put("endTime", a.getEndTime() != null ? a.getEndTime().toString() : null);
        return data;
    }
}
