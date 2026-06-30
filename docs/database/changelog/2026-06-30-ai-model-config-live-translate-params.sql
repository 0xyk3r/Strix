-- 2026-06-30 AI 模型配置新增实时语音翻译参数字段
ALTER TABLE sys_ai_model_config
    ADD COLUMN live_translate_params TEXT COMMENT '实时语音翻译默认参数（JSON文本，LiveTranslate专用）' AFTER tts_params;
