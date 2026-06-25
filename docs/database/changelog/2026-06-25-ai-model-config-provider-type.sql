-- 2026-06-25 AI 模型配置新增云提供商类型字段
ALTER TABLE sys_ai_model_config
    ADD COLUMN provider_type TINYINT DEFAULT 0 COMMENT '云提供商类型: 0=自动识别 1=DashScope 2=DeepSeek 3=OpenAI 9=其他兼容' AFTER `type`;
