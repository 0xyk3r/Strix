-- ============================================================
-- MySQL 8.0 系统日志表 DDL
-- 表名: sys_system_log
-- 说明: Strix 系统操作日志表，用于记录所有系统操作行为
-- 注意: 去除了 client_location 字段（内网环境IP定位无意义）
-- ============================================================

DROP TABLE IF EXISTS `sys_system_log`;

CREATE TABLE `sys_system_log`
(
    `id`               VARCHAR(64) NOT NULL COMMENT '主键ID',
    `app_id`           VARCHAR(100) DEFAULT NULL COMMENT '应用ID',
    `app_version`      VARCHAR(50)  DEFAULT NULL COMMENT '应用版本',

    -- 操作信息
    `operation_type`   VARCHAR(50)  DEFAULT NULL COMMENT '操作类型（如：CREATE、UPDATE、DELETE、QUERY等）',
    `operation_group`  VARCHAR(100) DEFAULT NULL COMMENT '操作分组（模块名称）',
    `operation_name`   VARCHAR(200) DEFAULT NULL COMMENT '操作名称（具体功能描述）',
    `operation_method` VARCHAR(10)  DEFAULT NULL COMMENT '请求方法（GET、POST、PUT、DELETE等）',
    `operation_url`    VARCHAR(500) DEFAULT NULL COMMENT '操作URL',
    `operation_param`  TEXT         DEFAULT NULL COMMENT '操作参数（JSON格式，已脱敏）',
    `operation_time`   DATETIME     DEFAULT NULL COMMENT '操作时间',
    `operation_spend`  BIGINT       DEFAULT NULL COMMENT '操作耗时（毫秒）',

    -- 客户端信息
    `client_ip`        VARCHAR(128) DEFAULT NULL COMMENT '客户端IP地址',
    `client_device`    VARCHAR(200) DEFAULT NULL COMMENT '客户端设备（操作系统）',
    `client_user`      VARCHAR(64)  DEFAULT NULL COMMENT '操作用户ID',
    `client_username`  VARCHAR(200) DEFAULT NULL COMMENT '操作用户名称',

    -- 响应信息
    `response_code`    INT          DEFAULT NULL COMMENT '响应状态码',
    `response_msg`     VARCHAR(500) DEFAULT NULL COMMENT '响应消息',
    `response_data`    TEXT         DEFAULT NULL COMMENT '响应数据（JSON格式）',

    PRIMARY KEY (`id`),

    -- 核心查询索引：按时间倒序查询
    KEY                `idx_operation_time` (`operation_time` DESC),

    -- 复合索引：按用户查询操作记录
    KEY                `idx_user_time` (`client_user`, `operation_time` DESC),

    -- 复合索引：按操作类型查询
    KEY                `idx_type_time` (`operation_type`, `operation_time` DESC),

    -- 复合索引：按URL查询（适用于特定接口的日志追踪）
    KEY                `idx_url_time` (`operation_url`(100), `operation_time` DESC),

    -- 普通索引：按IP查询（用于安全审计）
    KEY                `idx_client_ip` (`client_ip`)

) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='系统操作日志表';


-- ============================================================
-- 分区策略（推荐）
-- 说明: 按月分区，便于归档和删除旧数据
-- 注意: 分区字段必须是主键或唯一索引的一部分，因此需要调整主键
-- ============================================================

-- 方案1：使用 RANGE 分区（推荐用于生产环境）
-- 需要先删除表，重新创建带分区的表

DROP TABLE IF EXISTS `sys_system_log`;

CREATE TABLE `sys_system_log`
(
    `id`               VARCHAR(64) NOT NULL COMMENT '主键ID',
    `app_id`           VARCHAR(100) DEFAULT NULL COMMENT '应用ID',
    `app_version`      VARCHAR(50)  DEFAULT NULL COMMENT '应用版本',

    -- 操作信息
    `operation_type`   VARCHAR(50)  DEFAULT NULL COMMENT '操作类型',
    `operation_group`  VARCHAR(100) DEFAULT NULL COMMENT '操作分组',
    `operation_name`   VARCHAR(200) DEFAULT NULL COMMENT '操作名称',
    `operation_method` VARCHAR(10)  DEFAULT NULL COMMENT '请求方法',
    `operation_url`    VARCHAR(500) DEFAULT NULL COMMENT '操作URL',
    `operation_param`  TEXT         DEFAULT NULL COMMENT '操作参数（JSON格式，已脱敏）',
    `operation_time`   DATETIME    NOT NULL COMMENT '操作时间（NOT NULL，用于分区）',
    `operation_spend`  BIGINT       DEFAULT NULL COMMENT '操作耗时（毫秒）',

    -- 客户端信息
    `client_ip`        VARCHAR(128) DEFAULT NULL COMMENT '客户端IP地址',
    `client_device`    VARCHAR(200) DEFAULT NULL COMMENT '客户端设备',
    `client_user`      VARCHAR(64)  DEFAULT NULL COMMENT '操作用户ID',
    `client_username`  VARCHAR(200) DEFAULT NULL COMMENT '操作用户名称',

    -- 响应信息
    `response_code`    INT          DEFAULT NULL COMMENT '响应状态码',
    `response_msg`     VARCHAR(500) DEFAULT NULL COMMENT '响应消息',
    `response_data`    TEXT         DEFAULT NULL COMMENT '响应数据（JSON格式）',

    -- 主键必须包含分区字段
    PRIMARY KEY (`id`, `operation_time`),

    KEY                `idx_operation_time` (`operation_time` DESC),
    KEY                `idx_user_time` (`client_user`, `operation_time` DESC),
    KEY                `idx_type_time` (`operation_type`, `operation_time` DESC),
    KEY                `idx_url_time` (`operation_url`(100), `operation_time` DESC),
    KEY                `idx_client_ip` (`client_ip`)

) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='系统操作日志表（分区版）'

-- 按月分区（最近6个月 + 未来3个月）
PARTITION BY RANGE (TO_DAYS(operation_time)) (
    PARTITION p202501 VALUES LESS THAN (TO_DAYS('2025-02-01')),
    PARTITION p202502 VALUES LESS THAN (TO_DAYS('2025-03-01')),
    PARTITION p202503 VALUES LESS THAN (TO_DAYS('2025-04-01')),
    PARTITION p202504 VALUES LESS THAN (TO_DAYS('2025-05-01')),
    PARTITION p202505 VALUES LESS THAN (TO_DAYS('2025-06-01')),
    PARTITION p202506 VALUES LESS THAN (TO_DAYS('2025-07-01')),
    PARTITION p202507 VALUES LESS THAN (TO_DAYS('2025-08-01')),
    PARTITION p202508 VALUES LESS THAN (TO_DAYS('2025-09-01')),
    PARTITION p202509 VALUES LESS THAN (TO_DAYS('2025-10-01')),
    PARTITION p_future VALUES LESS THAN MAXVALUE
);


-- ============================================================
-- 分区维护脚本
-- ============================================================

-- 1. 查看当前分区信息
SELECT PARTITION_NAME,
       PARTITION_EXPRESSION,
       PARTITION_DESCRIPTION,
       TABLE_ROWS
FROM INFORMATION_SCHEMA.PARTITIONS
WHERE TABLE_SCHEMA = 'strix'
  AND TABLE_NAME = 'sys_system_log'
ORDER BY PARTITION_ORDINAL_POSITION;

-- 2. 添加新月份分区（每月执行一次，建议通过定时任务自动化）
-- 示例：添加 2025年10月的分区
ALTER TABLE sys_system_log
    REORGANIZE PARTITION p_future INTO (
    PARTITION p202510 VALUES LESS THAN (TO_DAYS('2025-11-01')),
    PARTITION p_future VALUES LESS THAN MAXVALUE
    );

-- 3. 删除旧分区数据（归档后删除，如删除6个月前的数据）
-- 示例：删除 2025年1月的分区
ALTER TABLE sys_system_log DROP PARTITION p202501;

-- 4. 归档旧数据到历史表（删除前建议先归档）
-- 创建归档表
CREATE TABLE sys_system_log_archive LIKE sys_system_log;

-- 归档指定分区的数据
INSERT INTO sys_system_log_archive
SELECT *
FROM sys_system_log PARTITION (p202501);


-- ============================================================
-- 数据库优化配置建议
-- ============================================================

-- 1. 确保 rewriteBatchedStatements 已启用（在 JDBC URL 中配置）
-- jdbc:mysql://host:port/db?rewriteBatchedStatements=true

-- 2. 调整 InnoDB 缓冲池大小（服务器总内存的 50-70%）
-- 在 my.cnf 或 my.ini 中配置：
-- [mysqld]
-- innodb_buffer_pool_size = 4G
-- innodb_log_file_size = 512M
-- innodb_flush_log_at_trx_commit = 2  # 日志表可以适当放宽持久性要求
-- innodb_flush_method = O_DIRECT

-- 3. 日志表专用优化
-- 如果日志写入量大，可以考虑：
-- - 使用独立的表空间
-- - 禁用二进制日志（如果不需要主从复制）
-- - 调整 innodb_io_capacity 提升IO吞吐
