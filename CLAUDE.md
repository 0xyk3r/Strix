# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Strix is a Java 21 / Spring Boot 3 business middleware framework (中台应用框架). It provides pluggable modules for SMS,
OSS, OAuth, Pay, Job scheduling, Captcha, and DelayedTask processing.

## Build Commands

```bash
# Build (with tests)
./gradlew build

# Build (skip tests)
./gradlew build -x test

# Create executable JAR
./gradlew bootJar

# Run tests
./gradlew test

# Run a single test class
./gradlew test --tests "cn.projectan.strix.SomeTestClass"

# GraalVM native image
./gradlew nativeCompile

# Docker image with native support
./gradlew bootBuildImage
```

## Architecture

### Package Structure (`cn.projectan.strix`)

- **controller/** - REST endpoints organized by domain (api/, system/, pay/, wechat/)
- **service/** - Business logic layer (base/, common/, system/)
- **mapper/** - MyBatis DAO interfaces
- **model/** - Data models
  - `db/` - Database entities
  - `request/` - Request DTOs
  - `response/` - Response DTOs
  - `dict/` - Dictionary models
  - `constant/` - Constants
  - `annotation/` - Custom annotations
  - `enums/` - Enumerations
  - `properties/` - Configuration property classes
- **core/** - Framework core
  - `ret/` - Response pattern (RetResult, RetCode, RetBuilder)
  - `ss/` - Spring Security integration
  - `encrypt/` - Field encryption system
  - `datamask/` - Data masking
  - `module/` - Pluggable modules (OAuth, OSS, Pay, SMS, Workflow)
  - `validation/` - Custom validators
  - `aop/` - Aspects and interceptors
- **util/** - Utilities organized by category (algo/, async/, encrypt/, http/, etc.)
- **aot/** - GraalVM AOT compilation features
- **config/** - Spring configuration classes

### Key Patterns

**Response Format**: All API responses use `RetResult<T>` with `RetCode` enums and `RetBuilder` for construction.

**Security**: Stateless token-based auth with two user types - SystemManager and SystemUser. CSRF disabled, CORS
enabled.

**Data Layer**: MyBatis Plus with automatic field encryption (`@EncryptField`), data masking, soft delete, audit
fields (createdBy/updatedBy/createdTime/updatedTime), and optimistic locking.

**Modules**: Enable/disable via `strix.module.*` configuration (sms, oss, job, oauth, push, pay).

### Tech Stack

- Spring Boot 3.5.8, Java 21
- MyBatis Plus 3.5.15 (MySQL 8+)
- Redis/Redisson for caching
- Knife4j 4.5.0 for API docs
- Quartz for job scheduling
- Hutool 5.8.42 utilities
- AWS SDK v2 for S3
- Alipay/WeChat SDKs for payments

## Code Generation

Use `MysqlGenerator.java` to generate entity/mapper/service boilerplate from database tables.

## Configuration

Profiles: `application-{dev,prod,test}.yml`

Key `strix.*` properties:

- `module.*` - Enable/disable modules
- `captcha.*` - CAPTCHA settings
- `delayed-task.*` - Async task processing
- `package-scan.job/model` - Dynamic class scanning
- `default-locale` - i18n (default: zh_CN)
