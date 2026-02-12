# MySQL 8.0 系统日志表优化指南

## 目录

1. [表结构设计](#1-表结构设计)
2. [索引优化策略](#2-索引优化策略)
3. [分区策略](#3-分区策略)
4. [批量插入优化](#4-批量插入优化)
5. [数据归档策略](#5-数据归档策略)
6. [MySQL配置优化](#6-mysql配置优化)
7. [监控与维护](#7-监控与维护)
8. [性能基准测试](#8-性能基准测试)

---

## 1. 表结构设计

### 1.1 字段优化要点

#### ✅ 已优化项

- **去除 `client_location` 字段**：内网环境IP定位无意义，减少存储和异步查询开销
- **使用 VARCHAR 替代 TEXT**：对于可控长度的字段（如 `operation_param`），建议限制最大长度
- **字符集选择**：使用 `utf8mb4_unicode_ci`，支持完整的Unicode字符集

#### ⚠️ 建议调整

**字段长度限制**（防止数据过大影响性能）：

```sql
-- 建议修改 SystemLog.java 实体，在 Mapper 层添加截断逻辑
operation_param
VARCHAR(2000)  -- 超过2000字符时截断
response_data      VARCHAR(5000)  -- 超过5000字符时截断
```

**在应用层实现截断**：

```java
// 在 SystemLogAspect.java 的 handleLog 方法中添加
private static final int MAX_PARAM_LENGTH = 2000;
private static final int MAX_RESPONSE_LENGTH = 5000;

private String truncateIfNeeded(String value, int maxLength) {
    if (value == null) {
        return null;
    }
    if (value.length() > maxLength) {
        return value.substring(0, maxLength - 15) + "...[truncated]";
    }
    return value;
}
```

### 1.2 字段类型选择

| 字段类型              | 推荐使用         | 原因                    |
|-------------------|--------------|-----------------------|
| `operation_time`  | DATETIME     | 不受时区影响，适合跨时区部署        |
| `operation_spend` | BIGINT       | 支持更大的耗时值（毫秒）          |
| `response_code`   | INT          | 兼容HTTP状态码和自定义业务码      |
| `client_ip`       | VARCHAR(128) | 支持IPv6地址（最长39字符）+ 代理链 |

---

## 2. 索引优化策略

### 2.1 核心索引

```sql
-- 1. 主查询索引：按时间倒序查询（最常用）
KEY `idx_operation_time` (`operation_time` DESC)

-- 2. 用户操作历史查询
KEY `idx_user_time` (`client_user`, `operation_time` DESC)

-- 3. 按操作类型查询
KEY `idx_type_time` (`operation_type`, `operation_time` DESC)

-- 4. URL追踪查询（前缀索引，节省空间）
KEY `idx_url_time` (`operation_url`(100), `operation_time` DESC)

-- 5. IP安全审计
KEY `idx_client_ip` (`client_ip`)
```

### 2.2 索引使用建议

#### ✅ 高效查询示例

```sql
-- 查询某用户最近的操作记录（使用 idx_user_time）
SELECT *
FROM sys_system_log
WHERE client_user = 'user123'
ORDER BY operation_time DESC LIMIT 100;

-- 查询某时间段的所有操作（使用 idx_operation_time）
SELECT *
FROM sys_system_log
WHERE operation_time BETWEEN '2025-01-01' AND '2025-01-31'
ORDER BY operation_time DESC;

-- 查询某接口的调用记录（使用 idx_url_time）
SELECT *
FROM sys_system_log
WHERE operation_url LIKE '/api/user/login%'
  AND operation_time > '2025-01-01'
ORDER BY operation_time DESC;
```

#### ❌ 避免的查询模式

```sql
-- ❌ 避免：对 TEXT 字段进行 LIKE 查询（无法使用索引）
SELECT *
FROM sys_system_log
WHERE operation_param LIKE '%password%';

-- ❌ 避免：不带时间条件的全表扫描
SELECT *
FROM sys_system_log
WHERE client_ip = '192.168.1.1';

-- ✅ 改进：添加时间范围限制
SELECT *
FROM sys_system_log
WHERE client_ip = '192.168.1.1'
  AND operation_time > DATE_SUB(NOW(), INTERVAL 7 DAY);
```

### 2.3 索引监控

```sql
-- 查看索引使用情况
SELECT index_name,
       seq_in_index,
       column_name,
       cardinality
FROM information_schema.STATISTICS
WHERE table_schema = 'strix'
  AND table_name = 'sys_system_log'
ORDER BY index_name, seq_in_index;

-- 分析未使用的索引（需开启 performance_schema）
SELECT *
FROM sys.schema_unused_indexes
WHERE object_schema = 'strix'
  AND object_name = 'sys_system_log';
```

---

## 3. 分区策略

### 3.1 为什么需要分区？

日志表数据增长快，分区可以：

1. **提升查询性能**：查询时只扫描相关分区
2. **简化维护**：删除旧数据只需 `DROP PARTITION`（秒级完成）
3. **减少锁竞争**：不同分区的操作可以并行

### 3.2 分区方案选择

#### 方案A：按月 RANGE 分区（推荐）

```sql
PARTITION
BY RANGE (TO_DAYS(operation_time)) (
    PARTITION p202501 VALUES LESS THAN (TO_DAYS('2025-02-01')),
    PARTITION p202502 VALUES LESS THAN (TO_DAYS('2025-03-01')),
    -- ...
    PARTITION p_future VALUES LESS THAN MAXVALUE
);
```

**优点**：

- 查询效率高（分区裁剪）
- 删除旧数据快速（`ALTER TABLE ... DROP PARTITION`）
- 适合时间范围查询

**缺点**：

- 需要定期维护（添加新分区）
- 主键必须包含分区字段 `operation_time`

#### 方案B：不使用分区

如果数据量不大（< 1000万行），可以不使用分区：

- 使用定期归档 + DELETE 清理旧数据
- 更简单的表结构

### 3.3 分区维护自动化

**创建存储过程自动添加分区**：

```sql
DELIMITER
$$

CREATE PROCEDURE maintain_log_partitions()
BEGIN
    DECLARE
next_month DATE;
    DECLARE
partition_name VARCHAR(20);
    DECLARE
partition_limit VARCHAR(20);

    -- 计算下个月
    SET
next_month = DATE_ADD(CURDATE(), INTERVAL 3 MONTH);
    SET
partition_name = CONCAT('p', DATE_FORMAT(next_month, '%Y%m'));
    SET
partition_limit = DATE_FORMAT(DATE_ADD(next_month, INTERVAL 1 MONTH), '%Y-%m-01');

    -- 检查分区是否存在
    IF
NOT EXISTS (
        SELECT 1 FROM information_schema.PARTITIONS
        WHERE TABLE_SCHEMA = 'strix'
          AND TABLE_NAME = 'sys_system_log'
          AND PARTITION_NAME = partition_name
    ) THEN
        SET @sql = CONCAT(
            'ALTER TABLE sys_system_log REORGANIZE PARTITION p_future INTO (',
            'PARTITION ', partition_name, ' VALUES LESS THAN (TO_DAYS(''', partition_limit, ''')),',
            'PARTITION p_future VALUES LESS THAN MAXVALUE)'
        );
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
END IF;
END$$

DELIMITER ;

-- 创建定时事件（每月1号执行）
CREATE
EVENT IF NOT EXISTS auto_add_log_partition
ON SCHEDULE EVERY 1 MONTH
STARTS '2025-02-01 00:00:00'
DO CALL maintain_log_partitions();
```

### 3.4 删除旧分区

```sql
-- 归档后删除（保留6个月数据）
ALTER TABLE sys_system_log DROP PARTITION p202501;
```

---

## 4. 批量插入优化

### 4.1 JDBC URL 配置

确保启用批量重写（已在代码中配置）：

```properties
# application-dev.yml
spring.datasource.url=jdbc:mysql://host:port/db?rewriteBatchedStatements=true&autoReconnect=true
```

### 4.2 批量大小调优

当前配置（`AsyncSystemLogServiceImpl.java`）：

```java
private static final int BATCH_SIZE = 100;  // 每批100条

@Scheduled(fixedDelay = 5000)  // 每5秒执行一次
public void batchSaveLogs() {
    // ...
    systemLogService.saveBatch(logs);
}
```

**调优建议**：

| 场景               | BATCH_SIZE | fixedDelay | 说明        |
|------------------|------------|------------|-----------|
| 低并发（<100 QPS）    | 50         | 10000ms    | 减少数据库连接频率 |
| 中并发（100-500 QPS） | 100        | 5000ms     | 当前配置（推荐）  |
| 高并发（>500 QPS）    | 200        | 3000ms     | 更频繁的批量插入  |

### 4.3 性能对比

| 插入方式         | 1000条耗时 | 10000条耗时 |
|--------------|---------|----------|
| 单条插入         | ~2000ms | ~20000ms |
| 批量插入（100条/批） | ~200ms  | ~2000ms  |
| **性能提升**     | **10倍** | **10倍**  |

---

## 5. 数据归档策略

### 5.1 归档流程

```
定期检查 → 归档到历史表 → 删除旧分区 → 压缩归档数据
```

### 5.2 归档脚本

**Step 1: 创建归档表**

```sql
-- 与主表结构相同，但不需要复杂索引
CREATE TABLE sys_system_log_archive
(
    -- 与 sys_system_log 相同的字段
    .
    .
    .
    PRIMARY
    KEY
(
    id,
    operation_time
),
    KEY idx_operation_time (operation_time DESC)
    ) ENGINE=InnoDB PARTITION BY RANGE (TO_DAYS(operation_time)) (
    PARTITION p_archive VALUES LESS THAN MAXVALUE
);
```

**Step 2: 归档数据**

```sql
-- 归档3个月前的数据
INSERT INTO sys_system_log_archive
SELECT *
FROM sys_system_log
WHERE operation_time < DATE_SUB(CURDATE(), INTERVAL 3 MONTH);

-- 验证归档数据量
SELECT COUNT(*)
FROM sys_system_log_archive;

-- 删除已归档的分区
ALTER TABLE sys_system_log DROP PARTITION p202501;
```

**Step 3: 导出归档数据（可选）**

```bash
# 导出为SQL文件
mysqldump -u root -p strix sys_system_log_archive \
  --where="operation_time >= '2025-01-01' AND operation_time < '2025-02-01'" \
  > sys_log_202501.sql

# 压缩归档
gzip sys_log_202501.sql
```

### 5.3 自动化归档

```sql
DELIMITER
$$

CREATE PROCEDURE archive_old_logs(IN months_to_keep INT)
BEGIN
    DECLARE
archive_date DATE;
    SET
archive_date = DATE_SUB(CURDATE(), INTERVAL months_to_keep MONTH);

    -- 归档
INSERT INTO sys_system_log_archive
SELECT *
FROM sys_system_log
WHERE operation_time < archive_date;

-- 删除（需手动指定分区）
-- ALTER TABLE sys_system_log DROP PARTITION pXXXXXX;
END$$

DELIMITER ;

-- 每月执行一次（保留6个月数据）
CREATE
EVENT IF NOT EXISTS auto_archive_logs
ON SCHEDULE EVERY 1 MONTH
STARTS '2025-01-01 02:00:00'
DO CALL archive_old_logs(6);
```

---

## 6. MySQL配置优化

### 6.1 InnoDB 核心参数

在 `my.cnf` 或 `my.ini` 中配置：

```ini
[mysqld]
# === InnoDB 缓冲池 ===
# 设置为物理内存的 50-70%（如服务器有8GB内存，设置为4-5GB）
innodb_buffer_pool_size = 4G
innodb_buffer_pool_instances = 4  # CPU核心数

# === 日志文件 ===
innodb_log_file_size = 512M  # 减少checkpoint频率
innodb_log_buffer_size = 16M
innodb_flush_log_at_trx_commit = 2  # 日志表可放宽持久性（性能 vs 安全性权衡）

# === IO性能 ===
innodb_io_capacity = 2000  # SSD推荐 2000-5000，HDD推荐 200
innodb_io_capacity_max = 4000
innodb_flush_method = O_DIRECT  # Linux环境，避免双重缓冲

# === 并发控制 ===
innodb_write_io_threads = 4
innodb_read_io_threads = 4

# === 表空间 ===
innodb_file_per_table = ON  # 每个表独立表空间
```

### 6.2 连接池配置

`application-dev.yml` 已配置（HikariCP）：

```yaml
spring:
  datasource:
    hikari:
      minimum-idle: 2
      maximum-pool-size: 10  # 日志表写入为主，不需要太多连接
      connection-timeout: 5000
      idle-timeout: 300000
      max-lifetime: 1800000
```

### 6.3 批量插入专用优化

```ini
[mysqld]
# 批量插入时临时禁用约束检查（仅限日志归档场景）
# innodb_autoinc_lock_mode = 2  # 使用交叉锁模式，提升并发插入性能
```

---

## 7. 监控与维护

### 7.1 表大小监控

```sql
-- 查看表和索引大小
SELECT table_name                                             AS `Table`,
       ROUND(((data_length + index_length) / 1024 / 1024), 2) AS `Size (MB)`,
       ROUND((data_length / 1024 / 1024), 2)                  AS `Data Size (MB)`,
       ROUND((index_length / 1024 / 1024), 2)                 AS `Index Size (MB)`,
       table_rows                                             AS `Rows`
FROM information_schema.TABLES
WHERE table_schema = 'strix'
  AND table_name = 'sys_system_log';
```

### 7.2 分区大小监控

```sql
SELECT PARTITION_NAME,
       TABLE_ROWS,
       AVG_ROW_LENGTH,
       ROUND(DATA_LENGTH / 1024 / 1024, 2) AS `Data Size (MB)`
FROM information_schema.PARTITIONS
WHERE TABLE_SCHEMA = 'strix'
  AND TABLE_NAME = 'sys_system_log'
ORDER BY PARTITION_ORDINAL_POSITION;
```

### 7.3 慢查询监控

```sql
-- 开启慢查询日志
SET
GLOBAL slow_query_log = ON;
SET
GLOBAL long_query_time = 1;  -- 记录超过1秒的查询
SET
GLOBAL log_queries_not_using_indexes = ON;

-- 查看慢查询
SELECT *
FROM mysql.slow_log
ORDER BY start_time DESC LIMIT 10;
```

### 7.4 Java应用层监控

在 `AsyncSystemLogServiceImpl` 中添加监控：

```java

@Scheduled(fixedDelay = 60000)  // 每分钟输出一次
public void logQueueMetrics() {
    int queueSize = logQueue.size();
    log.info("System log queue size: {}/{}", queueSize, QUEUE_CAPACITY);

    if (queueSize > QUEUE_CAPACITY * 0.8) {
        log.warn("System log queue is nearly full! Consider increasing batch size or frequency.");
    }
}
```

---

## 8. 性能基准测试

### 8.1 写入性能测试

**测试场景**：并发插入10万条日志

```java

@Test
public void testBatchInsertPerformance() {
    List<SystemLog> logs = new ArrayList<>();
    for (int i = 0; i < 100000; i++) {
        SystemLog log = new SystemLog();
        // ... 填充数据
        logs.add(log);
    }

    long start = System.currentTimeMillis();
    systemLogService.saveBatch(logs, 1000);  // 每批1000条
    long end = System.currentTimeMillis();

    System.out.println("Batch insert 100,000 logs: " + (end - start) + "ms");
}
```

**预期结果**（MySQL 8.0 + SSD）：

| 数据量  | 批量大小 | 耗时  | TPS    |
|------|------|-----|--------|
| 10万条 | 100  | ~5秒 | 20,000 |
| 10万条 | 500  | ~3秒 | 33,000 |
| 10万条 | 1000 | ~2秒 | 50,000 |

### 8.2 查询性能测试

```sql
-- 测试：查询最近1天的日志（无分区 vs 有分区）
EXPLAIN
SELECT *
FROM sys_system_log
WHERE operation_time > DATE_SUB(NOW(), INTERVAL 1 DAY)
ORDER BY operation_time DESC LIMIT 100;
```

**分区裁剪效果**：

- 无分区：扫描全表
- 有分区：只扫描当天和昨天的分区（扫描行数减少 90%+）

---

## 总结

### 优先级排序

| 优化项               | 优先级  | 难度 | 收益      |
|-------------------|------|----|---------|
| **启用批量插入**        | 🔴 高 | 低  | 10倍性能提升 |
| **创建核心索引**        | 🔴 高 | 低  | 查询性能提升  |
| **调整 InnoDB 缓冲池** | 🔴 高 | 中  | 整体性能提升  |
| **实施分区策略**        | 🟡 中 | 高  | 长期维护性提升 |
| **配置数据归档**        | 🟡 中 | 中  | 控制表大小   |
| **监控慢查询**         | 🟢 低 | 低  | 发现性能问题  |

### 快速检查清单

- [ ] JDBC URL 包含 `rewriteBatchedStatements=true`
- [ ] 创建了核心索引（`idx_operation_time`, `idx_user_time` 等）
- [ ] 配置了 InnoDB 缓冲池大小（>= 物理内存的50%）
- [ ] 启用了批量插入（`saveBatch`）
- [ ] 设置了日志队列监控
- [ ] 制定了数据归档策略（如保留6个月）
- [ ] 配置了分区自动维护（如果使用分区）
- [ ] 监控表大小增长趋势

### 预期性能指标

| 指标          | 目标值          |
|-------------|--------------|
| 单条日志插入延迟    | < 1ms（异步）    |
| 批量插入TPS     | > 20,000     |
| 查询响应时间（带索引） | < 100ms      |
| 表大小增长（月）    | < 5GB（取决于流量） |
| 队列积压（正常）    | < 10%        |

---

**最后更新**: 2025-12-17
**版本**: 1.0.0
**作者**: ProjectAn & Claude Sonnet 4.5
