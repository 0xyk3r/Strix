# Copilot Instructions — Strix Backend

## Build & Test

```bash
./gradlew build              # Build with tests
./gradlew build -x test      # Build without tests
./gradlew bootJar            # Executable JAR → Strix.jar
./gradlew test               # All tests
./gradlew test --tests "cn.projectan.strix.SomeTestClass"           # Single test class
./gradlew test --tests "cn.projectan.strix.SomeTestClass.someMethod" # Single test method
./gradlew nativeCompile      # GraalVM native image
./gradlew bootBuildImage     # Docker native image
```

Testing stack: JUnit 5 + Spring Boot Test + Spring Security Test.

## Architecture

Java 21 / Spring Boot 4.0.2 / Gradle — business middleware framework (v3.0.0).
Package namespace: `cn.projectan.strix`. Default port: **9889**. Default locale: **zh_CN**.

### Layered Structure

**Controller → Service → Mapper → Database (MySQL 8+ / MyBatis Plus 3.5.16)**

- `controller/` — REST endpoints. Organized by domain: `system/` (admin), `srv/` (client-facing), `pay/` (payment
  callbacks). **All controllers must inherit `BaseController`** (directly or via `BaseSystemController` /
  `BaseSrvController`).
- `service/` — Business logic layer.
- `mapper/` — MyBatis DAO interfaces. XML mappings in `resources/mapper/system/`.
- `model/` — Subdivided into: `db/` (entities), `request/` (request DTOs), `response/` (response DTOs), `dict/`,
  `constant/`, `annotation/`, `enums/`, `properties/`, `event/`.
- `core/` — Framework internals (see below).
- `util/` — Utility classes organized by concern (crypto, http, file, ip, text, etc.).
- `config/` — Spring configuration classes (Security, Redis, MybatisPlus, CORS, Jackson, OpenAPI, etc.).
- `aot/` — GraalVM AOT features (lambda registration, BouncyCastle).

### Core Framework (`core/`)

| Package        | Purpose                                                                                                                                                                    |
|----------------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `ret/`         | Unified response: `RetResult<T>` + `RetCode` + `RetBuilder`. **All APIs must return `RetResult<T>`.**                                                                      |
| `ss/`          | Spring Security — stateless token auth for two user types: `SystemManager` and `SystemUser`. Each has its own token filter, authentication token, and login details class. |
| `module/`      | Pluggable modules via factory pattern — `OAuthClientFactory`, `OssClientFactory`, `PayClientFactory`, `SmsClientFactory`. Enabled via `strix.module.*` config.             |
| `encrypt/`     | Field-level auto encryption/decryption via `@EncryptField` annotation (MyBatis interceptor).                                                                               |
| `datamask/`    | Data masking via `@DataMask` annotation (Jackson serializer).                                                                                                              |
| `ratelimit/`   | API rate limiting via `@RateLimit` annotation (default: 600 req / 60s).                                                                                                    |
| `xss/`         | XSS protection filter and deserializer.                                                                                                                                    |
| `captcha/`     | Block puzzle CAPTCHA with SM4 encryption and Redis caching.                                                                                                                |
| `aop/`         | `@StrixLog` audit logging aspect, API security check aspect, global exception handler, request/response encoding advice.                                                   |
| `validation/`  | Custom validators: `@PasswordComplexity`, `@ConstantDictValue`, `@DynamicDictValue`. Validation groups: `InsertGroup`, `UpdateGroup`.                                      |
| `cache/`       | System-level caches (Region, Permission, Menu, Config).                                                                                                                    |
| `delayedtask/` | Redis-based delayed task processing.                                                                                                                                       |
| `security/`    | API security signature verification (`ApiSecurity`).                                                                                                                       |

### Custom Annotations

| Annotation                           | Purpose                                  |
|--------------------------------------|------------------------------------------|
| `@EncryptField`                      | Auto encrypt/decrypt string fields in DB |
| `@StrixLog`                          | Audit logging with operation metadata    |
| `@RateLimit`                         | API rate limiting                        |
| `@StrixJob`                          | Quartz job scheduling marker             |
| `@UniqueField` / `@UniqueDetections` | Unique constraint validation             |
| `@Anonymous`                         | Allow unauthenticated access             |
| `@IgnoreEncryption`                  | Exclude from encryption processing       |
| `@Dict` / `@DictData`                | Dictionary data binding                  |
| `@UpdateField`                       | Field-level update tracking              |

## Key Conventions

- **Entity base class**: All DB entities extend `BaseModel<T>` which provides: `id` (ASSIGN_ID strategy),
  `deletedStatus` (logical delete), `createdTime`, `createdBy`, `createdByType`, `updatedTime`, `updatedBy`,
  `updatedByType`.
- **Lombok + chain accessors**: All entities use `@Accessors(chain = true)`.
- **Response pattern**: Always use `RetBuilder` to build responses. Status codes are in `RetCode` (200, 400, 401, 403,
  404, 405, 429, 500).
- **Module enablement**: Modules (OAuth, OSS, Pay, SMS) are conditionally loaded via `strix.module.*` configuration
  properties.
- **Configuration prefix**: All Strix-specific config uses `strix.*` prefix.
- **Profiles**: `dev`, `prod`, `test` via `application-{profile}.yml`.
- **Internationalization**: Resources in `resources/i18n/strix/`, uses Hutool message interpolation.
- **Security**: CSRF disabled, CORS enabled, stateless token authentication — no server-side sessions.
- **Virtual threads**: Enabled in Spring Boot configuration.

## Key Dependencies

- MyBatis Plus 3.5.16, Redisson 4.1.0, Quartz (job scheduling)
- Knife4j 4.5.0 + SpringDoc OpenAPI 3.0.1 (API docs)
- Hutool 5.8.43 (utility), BouncyCastle 1.83 (SM3/SM4 cryptography)
- Alipay SDK + IJPay, Aliyun SMS SDK, AWS S3 SDK v2
- OkHttp 5.3.2, P6Spy 3.9.1 (dev SQL logging), Lombok

## Entry Point

`StrixApplication.java` — annotations: `@EnableAsync`, `@EnableCaching`, `@EnableScheduling`,
`@EnableAspectJAutoProxy(exposeProxy = true)`,
`@SpringBootApplication(proxyBeanMethods = false, scanBasePackages = "cn.projectan")`.
