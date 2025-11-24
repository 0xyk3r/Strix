# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Strix is a business application framework built on Java 21 and Spring Boot 3.4.5. It provides modular functionality
including SMS, OSS, OAuth, Push, Job scheduling, and Payment integrations.

## Build Commands

```bash
# Build the project
./gradlew build

# Build executable JAR (outputs Strix.jar)
./gradlew bootJar

# Build native image with GraalVM
./gradlew nativeCompile

# Build Docker image with native compilation
./gradlew bootBuildImage

# Run the application (AOT is auto-skipped)
./gradlew bootRun
```

Note: Tests are disabled by default in build.gradle (`test.enabled = false`).

## Code Generation

MyBatis-Plus code generators are available for database entities:

- `MysqlGenerator.java` - Generate code from MySQL tables
- `ClickHouseGenerator.java` - Generate code from ClickHouse tables

Run directly in IDE. Templates are in `src/main/resources/templates/mp/`.

## Architecture

### Package Structure (`cn.projectan.strix`)

- **aot/** - GraalVM native image features (Lambda registration, BouncyCastle)
- **config/** - Spring configurations (Security, Redis, Jackson, MyBatis-Plus, CORS, Captcha)
- **controller/** - REST controllers, `BaseController` is the parent class
- **core/** - Framework core functionality:
    - `aop/advice/` - Request/response encoding, global exception handling
    - `aop/aspect/` - Security check, logging aspects
    - `cache/` - System-level caches (config, menu, permission, region, workflow)
    - `captcha/` - Captcha service (block puzzle implementation)
    - `datamask/` - Data masking for sensitive fields via `@DataMask` annotation
    - `exception/` - Custom exceptions (`StrixException`, `StrixJobException`, `StrixNoAuthException`)
    - `module/` - Pluggable modules (oauth, oss, pay, sms, workflow)
    - `ret/` - API response builders (`RetBuilder`, `RetCode`)
    - `security/` - Security utilities
    - `ss/` - Spring Security token/details classes
    - `validation/` - Custom validators and validation groups
- **initializer/** - Module initializers (Banner, OAuth, OSS, Pay, SMS)
- **job/** - Scheduled job handlers
- **mapper/** - MyBatis-Plus mappers
- **model/** - Data models:
    - `annotation/` - Custom annotations (`@StrixJob`, `@StrixLog`, `@Anonymous`, `@NeedSystemPermission`)
    - `constant/` - Constants
    - `db/` - Database entities (extend `BaseModel`)
    - `dict/` - Dictionary models
    - `enums/` - Enumerations
    - `properties/` - Configuration properties classes
    - `request/` - Request DTOs
    - `response/` - Response DTOs
- **service/** - Service interfaces with implementations in `service/impl/`
- **task/** - Async tasks
- **util/** - Utility classes (Redis, Security, OkHttp, Token, i18n, etc.)

### Key Patterns

1. **Module Toggle**: Modules are enabled/disabled via `strix.module.*` properties in application.yml
2. **Entity Generation**: Use MyBatis-Plus generator; entities extend `BaseModel` with auto-filled fields (id,
   created_time, updated_time, deleted_status)
3. **Annotations**:
    - `@Anonymous` - Skip authentication
    - `@StrixJob` - Mark job handler methods
    - `@StrixLog` - Enable operation logging
    - `@NeedSystemPermission` - Require specific permissions
4. **Response Format**: Use `RetBuilder` for consistent API responses
5. **i18n**: Messages in `src/main/resources/i18n/strix*.properties`

### Database

- Primary: MySQL with MyBatis-Plus
- Analytics: ClickHouse support
- Dynamic datasource via `dynamic-datasource-spring-boot3-starter`
- SQL logging via P6Spy in development

### Configuration Profiles

- `application.yml` - Base config
- `application-dev.yml` - Development
- `application-test.yml` - Testing
- `application-prod.yml` - Production
