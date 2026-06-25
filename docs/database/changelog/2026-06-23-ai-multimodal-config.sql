-- 2026-06-23 AI 模型多模态能力配置扩展
-- 为 sys_ai_model_config 表新增多模态支持、生成参数、思考控制、搜索、视觉参数等字段

-- 多模态支持
ALTER TABLE sys_ai_model_config
    ADD COLUMN supported_modalities VARCHAR(128) DEFAULT NULL COMMENT '支持的多模态输入 JSON数组: ["image","video","audio"]' AFTER system_prompt;

-- 生成参数扩展
ALTER TABLE sys_ai_model_config
    ADD COLUMN max_completion_tokens INT DEFAULT NULL COMMENT '最大输出Token(含思考链)' AFTER max_tokens;
ALTER TABLE sys_ai_model_config
    ADD COLUMN presence_penalty DECIMAL(4, 2) DEFAULT NULL COMMENT '存在惩罚 [-2.0, 2.0]' AFTER max_completion_tokens;
ALTER TABLE sys_ai_model_config
    ADD COLUMN frequency_penalty DECIMAL(4, 2) DEFAULT NULL COMMENT '频率惩罚 [-2.0, 2.0]' AFTER presence_penalty;
ALTER TABLE sys_ai_model_config
    ADD COLUMN repetition_penalty DECIMAL(4, 2) DEFAULT NULL COMMENT '重复惩罚 (>0)' AFTER frequency_penalty;
ALTER TABLE sys_ai_model_config
    ADD COLUMN top_k INT DEFAULT NULL COMMENT '候选Token数' AFTER repetition_penalty;
ALTER TABLE sys_ai_model_config
    ADD COLUMN seed BIGINT DEFAULT NULL COMMENT '随机种子' AFTER top_k;
ALTER TABLE sys_ai_model_config
    ADD COLUMN n TINYINT DEFAULT NULL COMMENT '生成响应数量 [1-4]' AFTER seed;
ALTER TABLE sys_ai_model_config
    ADD COLUMN stop_sequences VARCHAR(512) DEFAULT NULL COMMENT '停止词 JSON数组' AFTER n;
ALTER TABLE sys_ai_model_config
    ADD COLUMN response_format VARCHAR(32) DEFAULT NULL COMMENT '输出格式: text/json_object' AFTER stop_sequences;
ALTER TABLE sys_ai_model_config
    ADD COLUMN logprobs TINYINT(1) DEFAULT NULL COMMENT '是否返回Token对数概率' AFTER response_format;
ALTER TABLE sys_ai_model_config
    ADD COLUMN top_logprobs TINYINT DEFAULT NULL COMMENT '候选Token概率数 [0-5]' AFTER logprobs;

-- 思考控制扩展 (enable_thinking 已存在，在其后新增)
ALTER TABLE sys_ai_model_config
    ADD COLUMN thinking_budget INT DEFAULT NULL COMMENT '思考过程最大Token数' AFTER enable_thinking;
ALTER TABLE sys_ai_model_config
    ADD COLUMN preserve_thinking TINYINT(1) DEFAULT NULL COMMENT '是否传递历史思考过程' AFTER thinking_budget;
ALTER TABLE sys_ai_model_config
    ADD COLUMN reasoning_effort VARCHAR(16) DEFAULT NULL COMMENT '推理力度: high/max' AFTER preserve_thinking;

-- 联网搜索扩展 (enable_source 已存在，在其后新增)
ALTER TABLE sys_ai_model_config
    ADD COLUMN forced_search TINYINT(1) DEFAULT NULL COMMENT '强制联网搜索' AFTER enable_source;
ALTER TABLE sys_ai_model_config
    ADD COLUMN search_freshness INT DEFAULT NULL COMMENT '搜索时效: 7/30/180/365' AFTER forced_search;
ALTER TABLE sys_ai_model_config
    ADD COLUMN enable_search_extension TINYINT(1) DEFAULT NULL COMMENT '垂域搜索' AFTER search_freshness;

-- 视觉/多模态参数
ALTER TABLE sys_ai_model_config
    ADD COLUMN vl_high_resolution_images TINYINT(1) DEFAULT NULL COMMENT '高分辨率图像' AFTER enable_search_extension;
ALTER TABLE sys_ai_model_config
    ADD COLUMN min_pixels INT DEFAULT NULL COMMENT '图像最小像素阈值' AFTER vl_high_resolution_images;
ALTER TABLE sys_ai_model_config
    ADD COLUMN max_pixels INT DEFAULT NULL COMMENT '图像最大像素阈值' AFTER min_pixels;
ALTER TABLE sys_ai_model_config
    ADD COLUMN video_fps DECIMAL(4, 2) DEFAULT NULL COMMENT '视频抽帧频率 [0.1-10]' AFTER max_pixels;

-- 其他能力
ALTER TABLE sys_ai_model_config
    ADD COLUMN enable_text_image_mixed TINYINT(1) DEFAULT NULL COMMENT '图文混合输出' AFTER video_fps;
