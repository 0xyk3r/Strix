-- 2026-06-25 AI 消息缓存 Token 统计字段扩展
-- 为 sys_ai_message 表新增缓存命中、缓存写入、思考链 Token 统计字段

ALTER TABLE sys_ai_message
    ADD COLUMN cache_hit_tokens INT DEFAULT NULL COMMENT 'KV缓存命中Token数 (prompt_tokens_details.cached_tokens)' AFTER completion_tokens,
    ADD COLUMN cache_write_tokens INT DEFAULT NULL COMMENT '缓存写入Token数 (DashScope cache_write_tokens，有独立计费)'  AFTER cache_hit_tokens,
    ADD COLUMN reasoning_tokens   INT DEFAULT NULL COMMENT '思考链Token数 (completion_tokens_details.reasoning_tokens，含于completionTokens内)' AFTER cache_write_tokens;
