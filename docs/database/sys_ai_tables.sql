-- ============================================================
-- MySQL 8.0 AI 模块建表 DDL
-- 版本: 3.0.0
-- 说明: Strix AI 模块 - 模型配置、会话、消息表
-- ============================================================

-- AI 模型配置表
DROP TABLE IF EXISTS `sys_ai_model_config`;

CREATE TABLE `sys_ai_model_config`
(
    `id`                      VARCHAR(32)  NOT NULL COMMENT '主键 ID（雪花）',

    -- 基础信息
    `key`                     VARCHAR(64)  NOT NULL COMMENT '配置唯一标识 Key',
    `name`                    VARCHAR(128) NOT NULL COMMENT '配置显示名称',
    `type` TINYINT NOT NULL COMMENT '模型类型：1=TEXT 2=VISION 3=TTS 4=STT(离线) 5=IMAGE_GEN 6=ASR(实时)',

    -- 连接信息
    `base_url`                VARCHAR(512) NOT NULL COMMENT 'OpenAI 兼容端点 Base URL',
    `api_key`                 VARCHAR(512) NOT NULL COMMENT 'API Key',
    `model_name`              VARCHAR(128) NOT NULL COMMENT '模型标识（如 qwen3-max）',

    -- 文本/视觉参数
    `temperature`             DECIMAL(4, 2)         DEFAULT NULL COMMENT '温度（0.0-2.0，TEXT/VISION/STT）',
    `top_p`                   DECIMAL(4, 2)         DEFAULT NULL COMMENT 'TopP（0.0-1.0，TEXT/VISION）',
    `max_tokens`              INT                   DEFAULT NULL COMMENT '最大输出 Token 数',
    `system_prompt`           TEXT                  DEFAULT NULL COMMENT '系统提示词（TEXT/VISION）',

    -- Thinking 模式参数（TEXT 专用）
    `enable_thinking`         TINYINT(1)     DEFAULT 0 COMMENT '是否启用思考模式（qwen3 thinking）',
    `thinking_budget`         INT                   DEFAULT NULL COMMENT '思考模式 Token 预算',
    `enable_code_interpreter` TINYINT(1)   DEFAULT 0 COMMENT '是否启用代码解释器（TEXT 流式专用，需同时开启思考模式）',

    -- 联网搜索参数（TEXT 专用）
    `enable_search`           TINYINT(1)     DEFAULT 0 COMMENT '是否启用联网搜索',
    `search_strategy`         VARCHAR(16)           DEFAULT NULL COMMENT '搜索策略：auto/standard/max/agent',
    `enable_source`           TINYINT(1)     DEFAULT 0 COMMENT '是否在响应中附带搜索来源信息',

    -- TTS 参数
    `voice`                   VARCHAR(64)           DEFAULT NULL COMMENT '语音名称（TTS 专用）',
    `speed`                   DECIMAL(4, 2)         DEFAULT NULL COMMENT '语速（TTS 专用，0.25-4.0）',

    -- TTS/STT 通用参数
    `response_format`         VARCHAR(16)           DEFAULT NULL COMMENT '响应格式（TTS: mp3/wav；STT: json/text）',

    -- STT 参数
    `language`                VARCHAR(16)           DEFAULT NULL COMMENT '识别语言（STT 专用，如 zh、en）',

    -- STT 专用：OSS 配置
    `oss_config_key`  VARCHAR(64)   DEFAULT NULL COMMENT 'STT 专用：OSS 配置 Key',
    `oss_bucket_name` VARCHAR(128)  DEFAULT NULL COMMENT 'STT 专用：OSS 桶名称',

    -- 语音可变参数（JSON 文本，会话/请求级覆盖此默认）
    `asr_params`      VARCHAR(2048) DEFAULT NULL COMMENT 'ASR run-task 默认参数(JSON)',
    `stt_params`      VARCHAR(2048) DEFAULT NULL COMMENT 'STT 离线默认参数(JSON)',
    `tts_params`      VARCHAR(2048) DEFAULT NULL COMMENT 'TTS 合成默认参数(JSON)',

    -- 状态
    `status`                  TINYINT      NOT NULL DEFAULT 1 COMMENT '状态：0=禁用 1=启用',
    `remark`                  VARCHAR(512)          DEFAULT NULL COMMENT '备注',

    -- 公共字段
    `deleted_status`          TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除：0=正常 1=已删除',
    `created_time`            DATETIME              DEFAULT NULL COMMENT '创建时间',
    `created_by_type`         TINYINT               DEFAULT NULL COMMENT '创建者类型',
    `created_by`              VARCHAR(32)           DEFAULT NULL COMMENT '创建者 ID',
    `updated_time`            DATETIME              DEFAULT NULL COMMENT '更新时间',
    `updated_by_type`         TINYINT               DEFAULT NULL COMMENT '更新者类型',
    `updated_by`              VARCHAR(32)           DEFAULT NULL COMMENT '更新者 ID',

    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_key` (`key`),
    KEY                       `idx_type_status` (`type`, `status`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci
  COMMENT = 'AI 模型配置表';


-- AI TTS 自定义音色表（声音复刻 / 声音设计）
DROP TABLE IF EXISTS `sys_ai_tts_voice`;

CREATE TABLE `sys_ai_tts_voice`
(
    `id`               VARCHAR(32)  NOT NULL COMMENT '主键 ID（雪花）',
    `config_id`        VARCHAR(32)  NOT NULL COMMENT '关联 TTS 模型配置 ID',
    `config_key`       VARCHAR(64)  NOT NULL COMMENT '关联 TTS 模型配置 Key',
    `voice_id`         VARCHAR(128) NOT NULL COMMENT 'DashScope 音色 ID（voice_id）',
    `name`             VARCHAR(128) NOT NULL COMMENT '音色显示名称',
    `voice_type`       TINYINT      NOT NULL COMMENT '音色类型：1=声音复刻 2=声音设计',
    `target_model`     VARCHAR(128) NOT NULL COMMENT '绑定的语音合成模型（如 cosyvoice-v3.5-plus）',
    `prompt_audio_url` VARCHAR(1024)         DEFAULT NULL COMMENT '复刻参考音频 URL（声音复刻）',
    `voice_prompt`     VARCHAR(2048)         DEFAULT NULL COMMENT '声音描述文本（声音设计）',
    `preview_text`     VARCHAR(512)          DEFAULT NULL COMMENT '预览文本（声音设计）',
    `status`           VARCHAR(16)  NOT NULL DEFAULT 'DEPLOYING' COMMENT '音色状态：DEPLOYING/OK/UNDEPLOYED',
    `remark`           VARCHAR(512)          DEFAULT NULL COMMENT '备注',

    -- 公共字段
    `deleted_status`   TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除：0=正常 1=已删除',
    `created_time`     DATETIME              DEFAULT NULL COMMENT '创建时间',
    `created_by_type`  TINYINT               DEFAULT NULL COMMENT '创建者类型',
    `created_by`       VARCHAR(32)           DEFAULT NULL COMMENT '创建者 ID',
    `updated_time`     DATETIME              DEFAULT NULL COMMENT '更新时间',
    `updated_by_type`  TINYINT               DEFAULT NULL COMMENT '更新者类型',
    `updated_by`       VARCHAR(32)           DEFAULT NULL COMMENT '更新者 ID',

    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_voice_id` (`voice_id`),
    KEY                `idx_config_id` (`config_id`),
    KEY                `idx_config_key_type` (`config_key`, `voice_type`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci
  COMMENT = 'AI TTS 自定义音色表（声音复刻/设计）';


-- AI 对话会话表
DROP TABLE IF EXISTS `sys_ai_session`;

CREATE TABLE `sys_ai_session`
(
    `id`              VARCHAR(32)  NOT NULL COMMENT '主键 ID（雪花）',
    `model_config_id` VARCHAR(32)  NOT NULL COMMENT '关联模型配置 ID',
    `manager_id`      VARCHAR(32)  NOT NULL COMMENT '所属管理员 ID',
    `title`           VARCHAR(256) NOT NULL COMMENT '会话标题',
    `status`          TINYINT      NOT NULL DEFAULT 0 COMMENT '状态：0=活跃 1=已归档',

    -- 公共字段
    `deleted_status`  TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除：0=正常 1=已删除',
    `created_time`    DATETIME              DEFAULT NULL COMMENT '创建时间',
    `created_by_type` TINYINT               DEFAULT NULL COMMENT '创建者类型',
    `created_by`      VARCHAR(32)           DEFAULT NULL COMMENT '创建者 ID',
    `updated_time`    DATETIME              DEFAULT NULL COMMENT '更新时间',
    `updated_by_type` TINYINT               DEFAULT NULL COMMENT '更新者类型',
    `updated_by`      VARCHAR(32)           DEFAULT NULL COMMENT '更新者 ID',

    PRIMARY KEY (`id`),
    KEY               `idx_manager_id` (`manager_id`),
    KEY               `idx_model_config_id` (`model_config_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci
  COMMENT = 'AI 对话会话表';


-- AI 对话消息表
DROP TABLE IF EXISTS `sys_ai_message`;

CREATE TABLE `sys_ai_message`
(
    `id`                VARCHAR(32) NOT NULL COMMENT '主键 ID（雪花）',
    `session_id`        VARCHAR(32) NOT NULL COMMENT '关联会话 ID',

    -- 消息内容
    `role`              VARCHAR(16) NOT NULL COMMENT '消息角色：user/assistant/system',
    `content`           LONGTEXT             DEFAULT NULL COMMENT '消息正文内容',
    `thinking_content`  LONGTEXT             DEFAULT NULL COMMENT '思考过程（qwen3 thinking 模式）',
    `attachments`       TEXT                 DEFAULT NULL COMMENT '附件（JSON 数组：[{type,url,mimeType,name}]）',

    -- Token 统计
    `prompt_tokens`     INT                  DEFAULT NULL COMMENT '输入 Token 消耗',
    `completion_tokens` INT                  DEFAULT NULL COMMENT '输出 Token 消耗',

    -- 状态
    `status`            TINYINT     NOT NULL DEFAULT 0 COMMENT '消息状态：0=生成中 1=完成 2=错误',
    `error_msg`         VARCHAR(512)         DEFAULT NULL COMMENT '错误信息',

    -- 公共字段
    `deleted_status`    TINYINT     NOT NULL DEFAULT 0 COMMENT '逻辑删除：0=正常 1=已删除',
    `created_time`      DATETIME             DEFAULT NULL COMMENT '创建时间',
    `created_by_type`   TINYINT              DEFAULT NULL COMMENT '创建者类型',
    `created_by`        VARCHAR(32)          DEFAULT NULL COMMENT '创建者 ID',
    `updated_time`      DATETIME             DEFAULT NULL COMMENT '更新时间',
    `updated_by_type`   TINYINT              DEFAULT NULL COMMENT '更新者类型',
    `updated_by`        VARCHAR(32)          DEFAULT NULL COMMENT '更新者 ID',

    PRIMARY KEY (`id`),
    KEY                 `idx_session_id` (`session_id`),
    KEY                 `idx_session_status` (`session_id`, `status`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci
  COMMENT = 'AI 对话消息表';
