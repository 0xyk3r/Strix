# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build Commands

```bash
./gradlew build              # Full build with tests
./gradlew build -x test      # Build without tests
./gradlew bootJar            # Generate executable JAR (Strix.jar)
./gradlew test               # Run all tests
./gradlew test --tests "cn.projectan.strix.SomeTestClass"  # Run specific test
./gradlew nativeCompile      # Build GraalVM native image
./gradlew bootBuildImage     # Build Docker image with native support
```

## Project Overview

Strix is a business middleware framework built on **Java 21 / Spring Boot 4**. It provides pluggable modules for SMS,
OSS, OAuth, Payment, Job scheduling, Captcha, and Delayed Task processing.

**Root package:** `cn.projectan.strix`

## Architecture

### Key Layers

- **controller/** - REST API controllers organized by domain (pay/, srv/, system/)
- **service/** - Business logic layer (base/, common/, system/)
- **mapper/** - MyBatis DAO interfaces
- **model/** - Data models (db/, request/, response/, annotation/, enums/, properties/)
- **core/** - Framework core (ret/, ss/, encrypt/, datamask/, module/, aop/, validation/)
- **util/** - Utility classes by function (algo/, async/, encrypt/, http/, etc.)
- **config/** - Spring configuration classes
- **aot/** - GraalVM AOT compilation support

### Core Patterns

**Unified Response Model:** All endpoints return `RetResult<T>` with `RetCode` enum for status codes, built via
`RetBuilder`.

**Token-Based Authentication:** Stateless security with two user types: `SystemManager` and `SystemUser`. CSRF disabled,
CORS enabled.

**Field-Level Encryption:** Use `@EncryptField` annotation on entity String fields for automatic encryption/decryption
at database layer.

**Audit Fields:** `createdTime`, `updatedTime`, `createdBy`, `updatedBy`, `createdByType`, `updatedByType` are
auto-filled. Use `deletedStatus` for soft deletes.

**Data Masking:** Use `@Sensitive` annotation on response DTO fields for automatic masking.

**Operation Logging:** Use `@StrixLog` annotation on controller methods for automatic audit logging.

### Pluggable Modules

Modules are toggled via `strix.module.*` configuration:

- `sms` - SMS sending (Aliyun)
- `oss` - Object storage (S3, Aliyun OSS, Local)
- `job` - Quartz job scheduling
- `oauth` - OAuth clients (WeChat MP/OA, Alipay)
- `push` - Push notifications
- `pay` - Payment processing (Alipay, WeChat Pay)

Module implementations are in `core/module/` with corresponding utilities in `util/module/`.

## Key Conventions

- All controllers must extend `BaseController`
- Database entities extend `BaseModel` and go in `model/db/`
- Request/Response DTOs go in `model/request/` and `model/response/`
- Custom annotations are in `model/annotation/`
- Configuration properties classes are in `model/properties/`

## Configuration

- **application.yml** - Base configuration (active profile: dev)
- **application-{dev,prod,test}.yml** - Environment-specific profiles
- Core config prefix: `strix.*`

## Code Generation

Run `MysqlGenerator.java` to generate Entity, Mapper, and Service code from database tables.

## Tech Stack

- Spring Boot 4, Java 21
- MyBatis Plus 3.5.16 (MySQL 8+)
- Redis / Redisson (caching)
- Quartz (job scheduling)
- AWS SDK v2 (S3), Aliyun OSS
- Alipay / WeChat Pay SDKs
- Knife4j / SpringDoc OpenAPI (API documentation)
- Bouncy Castle, Hutool (crypto/utilities)
