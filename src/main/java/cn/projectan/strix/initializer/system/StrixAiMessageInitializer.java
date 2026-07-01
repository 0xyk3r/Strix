package cn.projectan.strix.initializer.system;

import cn.projectan.strix.service.system.AiMessageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * AI 消息初始化器
 * <p>
 * 应用启动时清理残留的「生成中」assistant 消息：进程重启后，进行中生成的内存缓冲（{@code AiStreamRegistry}）
 * 已丢失，这些消息无法再续播，若保留 {@code GENERATING} 状态会让前端永久显示加载中。将其标记为出错（中断），
 * 用户可通过「重新生成」重试。
 *
 * @author ProjectAn
 * @since 2026-07-01
 */
@Slf4j
@Order(110)
@Component
@RequiredArgsConstructor
public class StrixAiMessageInitializer implements ApplicationRunner {

    private final AiMessageService aiMessageService;

    @Override
    public void run(@NonNull ApplicationArguments args) {
        try {
            int count = aiMessageService.markStaleGeneratingAsInterrupted();
            if (count > 0) {
                log.info("AI 消息初始化：已将 {} 条残留的「生成中」消息标记为中断", count);
            }
        } catch (Exception e) {
            log.error("AI 消息初始化清理僵尸生成中消息失败", e);
        }
    }
}
