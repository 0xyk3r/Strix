# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Strix 是一个基于 **Java 21 / Spring Boot 4.0.2** 的业务中台框架，提供可插拔模块（SMS、OSS、OAuth、Pay、Job、Captcha、DelayedTask）。版本
3.0.0，包名 `cn.projectan.strix`。

## Build & Test Commands

```bash
./gradlew build              # 构建（含测试）
./gradlew build -x test      # 构建（跳过测试）
./gradlew bootJar            # 生成可执行 JAR (Strix.jar)
./gradlew test               # 运行全部测试
./gradlew test --tests "cn.projectan.strix.SomeTestClass"  # 运行单个测试类
./gradlew nativeCompile      # GraalVM 原生镜像编译
./gradlew bootBuildImage     # Docker 原生镜像构建
```

测试框架：JUnit 5 + Spring Boot Test + Spring Security Test。

## Architecture

**分层架构**：Controller → Service → Mapper → Database (MySQL 8+, MyBatis Plus)

- **controller/** — REST 接口层，按业务域划分（srv/、system/、pay/）。所有 Controller 必须继承 `BaseController`。
- **service/** — 业务逻辑层（base/、common/、system/）
- **mapper/** — MyBatis DAO 接口，XML 映射文件在 `resources/mapper/system/`
- **model/** — 数据模型：`db/`(实体)、`request/`(请求DTO)、`response/`(响应DTO)、`dict/`、`constant/`、`annotation/`、`enums/`、
  `properties/`
- **core/** — 框架核心：
  - `ret/` — 统一响应（`RetResult<T>` + `RetCode` + `RetBuilder`），所有 API 必须返回 `RetResult<T>`
  - `ss/` — Spring Security 集成（无状态 Token 认证，支持 SystemManager/SystemUser 两类用户）
  - `module/` — 可插拔模块（oauth/oss/pay/sms），通过 `strix.module.*` 配置启用
  - `encrypt/` — 字段级加密（`@EncryptField`）
  - `datamask/` — 数据脱敏
  - `ratelimit/` — 接口限流（默认 600次/60秒）
  - `xss/` — XSS 防护
  - `captcha/` — 滑块验证码（AES 加密）
  - `aop/` — 切面与拦截器
  - `validation/` — 自定义校验器
- **util/** — 工具类（algo/async/crypto/encrypt/file/http/ip/text 等）
- **aot/** — GraalVM AOT 编译支持（BouncyCastleFeature、LambdaRegistrationFeature）
- **config/** — Spring 配置类（Security、Redis、MybatisPlus、CORS、Jackson、OpenAPI 等）

## Key Patterns

- **统一响应**：所有接口使用 `RetBuilder` 构建 `RetResult<T>` 返回。**所有 API 返回 HTTP 200**，错误码在 `RetResult.code` 中
- **Controller**：`@PreAuthorize("@ss.hasPermission('module:resource:action')")` 做权限校验，`@Anonymous` 标记免认证接口，
  `@StrixLog(operationGroup, operationName, operationType)` 做审计日志
- **Service**：`extends ServiceImpl<Mapper, Entity>`，使用 `lambdaQuery()` / `lambdaUpdate()` 链式查询，尽可能减少自定义
  XML 使用
- **BaseController**：空类，所有 Controller 必须直接或间接继承（GraalVM 原生编译要求）
- **DTO**：列表请求继承 `BasePageReq<Entity>`，创建/更新共用同一 DTO + `@Validated(InsertGroup/UpdateGroup.class)` 分组校验
- **更新**：`UpdateBuilder.build(entity, req)` 从 `@UpdateField` 字段构建 `LambdaUpdateWrapper`
- **唯一校验**：`UniqueChecker.check(entity)` 在 save/update 前调用
- **缓存**：`@Cacheable` / `@CacheEvict` / `@Caching` 注解，Redis 缓存
- **i18n**：消息模板格式 `{validation.required:field.user.nickname}`，通过 `I18nUtil.get("key")` 解析
- **数据层**：逻辑删除（`deleted_status`）、审计字段自动填充、ASSIGN_ID 主键
- **模块化**：`strix.module.*` 配置项控制模块启停

## Configuration

- `application.yml` — 基础配置（默认 dev profile），虚拟线程已启用
- `application-{dev,prod,test}.yml` — 环境配置（端口 9889）
- 核心配置前缀：`strix.*`（module/captcha/delayed-task/rate-limit/cors）
- 国际化：默认 `zh_CN`，资源文件在 `resources/i18n/`
