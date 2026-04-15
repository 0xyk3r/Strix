package cn.projectan.strix.model.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

/**
 * 字典数据变更事件
 * <p>
 * 独立事件类 (不继承 CacheInvalidationEvent), 因为字典缓存已通过 @CacheEvict 清除.
 * 此事件仅用于触发 SSE 广播通知前端刷新字典数据.
 *
 * @author ProjectAn
 * @since 2026-04-18
 */
@Getter
public class DictChangedEvent extends ApplicationEvent {

    /** 变更的字典 key */
    private final String dictKey;

    /** 变更原因: dict_saved, data_added, data_updated, data_deleted, dict_updated, dict_deleted */
    private final String reason;

    public DictChangedEvent(Object source, String dictKey, String reason) {
        super(source);
        this.dictKey = dictKey;
        this.reason = reason;
    }
}
