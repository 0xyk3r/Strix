package cn.projectan.strix.service.system;

import cn.projectan.strix.model.response.system.ai.AiTaskStatusResp;
import cn.projectan.strix.util.common.RedisUtil;
import cn.projectan.strix.util.system.SecurityUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import java.util.UUID;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * AI 异步长任务服务
 * <p>
 * 用于 TTS 音色注册(~5min)、批量 ASR 转写(~10min)等长耗时操作：提交即返回 taskId，
 * 后台（虚拟线程）执行，状态/结果写入 Redis，前端轮询查询，避免阻塞请求线程触发超时。
 * <p>
 * 任务按"用户 ID + taskId"构建 Redis key 进行隔离，天然防止越权查询他人任务。
 *
 * @author ProjectAn
 * @since 2026-06-17
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiTaskService {

    public static final String STATUS_PENDING = "PENDING";
    public static final String STATUS_RUNNING = "RUNNING";
    public static final String STATUS_SUCCEEDED = "SUCCEEDED";
    public static final String STATUS_FAILED = "FAILED";

    /**
     * 任务状态在 Redis 的存活时间（秒）：1 小时
     */
    private static final long TTL_SECONDS = 3600;
    private static final String KEY_PREFIX = "strix:ai:task:";

    private final RedisUtil redisUtil;

    @Qualifier("taskExecutor")
    private final Executor taskExecutor;

    /**
     * 提交一个异步长任务：立即返回 taskId，后台执行 work，成功/失败写入 Redis。
     * <p>任务归属当前登录用户（在调用线程捕获 ownerId），TTL 1 小时。
     *
     * @param type 任务类型标识（如 tts-enroll / stt-transcribe）
     * @param work 实际执行的工作，返回结果字符串（音色 ID 或识别文本）
     * @return 任务 ID
     */
    public String submit(String type, Supplier<String> work) {
        // 必须在调用线程（持有 SecurityContext）捕获 ownerId；异步线程内无法再获取
        String ownerId = SecurityUtil.getOperatorId();
        Assert.hasText(ownerId, "无法确定操作人，无法创建任务");
        String taskId = UUID.randomUUID().toString().replace("-", "");

        store(ownerId, new AiTaskStatusResp(taskId, type, STATUS_PENDING, null, null));
        taskExecutor.execute(() -> {
            store(ownerId, new AiTaskStatusResp(taskId, type, STATUS_RUNNING, null, null));
            try {
                String result = work.get();
                store(ownerId, new AiTaskStatusResp(taskId, type, STATUS_SUCCEEDED, result, null));
            } catch (Exception e) {
                log.error("AI 异步任务执行失败: type={}, taskId={}", type, taskId, e);
                store(ownerId, new AiTaskStatusResp(taskId, type, STATUS_FAILED, null, e.getMessage()));
            }
        });
        return taskId;
    }

    /**
     * 查询当前登录用户的任务状态（按 key 隔离，天然防越权）。不存在返回 {@code null}。
     */
    public AiTaskStatusResp get(String taskId) {
        String ownerId = SecurityUtil.getOperatorId();
        if (!StringUtils.hasText(ownerId) || !StringUtils.hasText(taskId)) {
            return null;
        }
        Object value = redisUtil.get(key(ownerId, taskId));
        return value instanceof AiTaskStatusResp resp ? resp : null;
    }

    private void store(String ownerId, AiTaskStatusResp state) {
        redisUtil.set(key(ownerId, state.getTaskId()), state, TTL_SECONDS, TimeUnit.SECONDS);
    }

    private String key(String ownerId, String taskId) {
        return KEY_PREFIX + ownerId + ":" + taskId;
    }
}
