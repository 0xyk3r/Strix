package cn.projectan.strix.core.module.ai;

import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

/**
 * AI 模块共享的 Jackson 3 ObjectMapper。
 * <p>
 * AI 相关类（对话、Provider、文档分析）此前各自 {@code new ObjectMapper()}，
 * 这里收敛为单一共享实例，减少重复构建。
 * <p>
 * 说明：本实例刻意保持 Jackson 默认配置，<b>不</b>复用容器内经 {@code JacksonConfig}
 * 定制的 Bean —— 后者带有 XSS 反序列化与数据脱敏定制，用于对外 HTTP 响应；
 * 而 AI 模块用它序列化发往上游模型 API 的请求体、解析上游响应，
 * 若复用会把 AI 生成内容里的 {@code < > &} 等转义，破坏代码/HTML 输出，两者语义不同不能混用。
 * </p>
 *
 * @author ProjectAn
 */
public final class AiJson {

    private static final ObjectMapper SHARED = JsonMapper.builder().build();

    private AiJson() {
    }

    /**
     * 获取 AI 模块共享的 ObjectMapper（Jackson 3，默认配置，线程安全）。
     *
     * @return 共享 ObjectMapper 实例
     */
    public static ObjectMapper mapper() {
        return SHARED;
    }
}
