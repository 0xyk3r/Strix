# Strix Backend

## Build & Test

```bash
./gradlew build              # Build with tests
./gradlew build -x test      # Build without tests
./gradlew bootJar            # Executable JAR → Strix.jar
./gradlew test               # All tests
./gradlew test --tests "cn.projectan.strix.SomeTestClass"  # Single test class
./gradlew nativeCompile      # GraalVM native image
```

Testing: JUnit 5 + Spring Boot Test. Use `@SpringBootTest` for integration tests.

## Architecture

Java 21 / Spring Boot 4.0.2 / Gradle. Package: `cn.projectan.strix`. Port: **9889**. Locale: **zh_CN**.
Virtual threads enabled. CSRF disabled, CORS enabled, stateless token auth (no server sessions).

**Layers:** Controller → Service → Mapper → MySQL 8+ (MyBatis Plus 3.5.16)

- `controller/` — By domain: `system/` (admin), `srv/` (client), `pay/` (callbacks). **All controllers must
  inherit `BaseController`** (empty class — required for GraalVM native builds).
- `service/` — All services `extends ServiceImpl<Mapper, Entity>`.
- `mapper/` — All extend `BaseMapper<Entity>`. XML files are empty — use MyBatis Plus lambda API only.
- `model/` — `db/` (entities), `request/` (DTOs), `response/` (DTOs), `dict/`, `constant/`, `enums/`, `annotation/`.
- `core/` — Framework: `ret/` (response), `ss/` (security), `module/` (pluggable modules), `encrypt/`, `ratelimit/`,
  `aop/`, `validation/`, `cache/`, etc.

## Controller Pattern

```java

@Operation(summary = "用户列表")
@GetMapping("")
@PreAuthorize("@ss.hasPermission('system:user')")
@StrixLog(operationGroup = "系统用户", operationName = "查询用户列表")
public RetResult<SystemUserListResp> getSystemUserList(SystemUserListReq req) {
    Page<SystemUser> page = systemUserService.listPage(req);
    return RetBuilder.success(new SystemUserListResp(page.getRecords(), page.getTotal()));
}

@PostMapping("update")
@PreAuthorize("@ss.hasPermission('system:user:add')")
@StrixLog(operationGroup = "系统用户", operationName = "新增用户", operationType = SystemLogOperType.ADD)
public RetResult<Object> update(@RequestBody @Validated(InsertGroup.class) SystemUserUpdateReq req) {
    // ...
    return RetBuilder.success();
}
```

- Use `@Anonymous` on endpoints that skip authentication (login, captcha, webhooks)
- Permission format: `module:resource:action` (e.g. `system:user:update`)
- `@StrixLog` params: `operationGroup` (module label), `operationName` (action label), `operationType` (
  ADD/UPDATE/DELETE/LOGIN)
- **All API responses use HTTP 200** — error codes go in `RetResult.code`, not HTTP status

## Service Pattern

```java

@Service
@RequiredArgsConstructor
public class SystemUserService extends ServiceImpl<SystemUserMapper, SystemUser> {

    public Page<SystemUser> listPage(SystemUserListReq req) {
        return lambdaQuery()
                .like(StringUtils.hasText(req.getKeyword()), SystemUser::getNickname, req.getKeyword())
                .eq(NumUtil.checkCategory(req.getStatus(), NumCategory.NON_NEGATIVE),
                        SystemUser::getStatus, req.getStatus())
                .orderByAsc(SystemUser::getCreatedTime)
                .page(req.getPage());
    }

    @Transactional(rollbackFor = Exception.class)
    @Caching(evict = {@CacheEvict(value = "strix:...", key = "...")})
    public void deleteWithRelations(SystemUser user) {
        removeById(user);
        // cascade cleanup...
    }
}
```

- Use `lambdaQuery()` for SELECT, `lambdaUpdate()` for UPDATE
- Use `@Transactional(rollbackFor = Exception.class)` for multi-step mutations
- Use `@Cacheable` / `@CacheEvict` / `@Caching` for Redis caching
- Inject dependencies via `@RequiredArgsConstructor` (final fields)

## Request/Response DTOs

```java
// List request — extend BasePageReq<Entity> for pagination
@Data
public class SystemUserListReq extends BasePageReq<SystemUser> {
    @Size(max = 64)
    private String keyword;
    private Short status;
}

// Create/Update request — same DTO, different validation groups
@Data
public class SystemUserUpdateReq {
    @NotEmpty(groups = {InsertGroup.class, UpdateGroup.class},
            message = "{validation.required:field.user.nickname}")
    @Size(groups = {InsertGroup.class, UpdateGroup.class}, min = 2, max = 16)
    @UpdateField
    private String nickname;

    @DynamicDictValue(groups = {InsertGroup.class, UpdateGroup.class}, dictName = "SystemUserStatus")
    @UpdateField
    private Short status;
}

// Response — constructor from entity
@Data
public class SystemUserResp {
    private String id;
    private String nickname;

    public SystemUserResp(SystemUser user) {
        this.id = user.getId();
        this.nickname = user.getNickname();
    }
}
```

- Use `@Validated(InsertGroup.class)` for creation, `@Validated(UpdateGroup.class)` for updates
- Use `UpdateBuilder.build(entity, req)` to create `LambdaUpdateWrapper` from `@UpdateField` fields
- Use `UniqueChecker.check(entity)` before save/update for unique constraint validation
- i18n message format: `{validation.required:field.user.nickname}` — resolve via `I18nUtil.get("key")`

## Entity Conventions

All DB entities extend `BaseModel<T>` providing:

- `id` — String, `@TableId(type = IdType.ASSIGN_ID)` (snowflake)
- `deletedStatus` — `@TableLogic` soft delete (0=normal, 1=deleted)
- `createdTime`, `createdBy`, `createdByType` — auto-filled on INSERT
- `updatedTime`, `updatedBy`, `updatedByType` — auto-filled on INSERT/UPDATE
- All setters return `T` for fluent chaining

Use `@EncryptField` on sensitive String fields for automatic DB encryption/decryption.
Use `@DataMask` on response fields for data masking during serialization.

## Custom Annotations

| Annotation                                  | Purpose                                  |
|---------------------------------------------|------------------------------------------|
| `@Anonymous`                                | Skip authentication                      |
| `@PreAuthorize("@ss.hasPermission('...')")` | Permission check (SpEL bean `@ss`)       |
| `@StrixLog`                                 | Audit logging                            |
| `@RateLimit`                                | API rate limiting (default: 600/60s)     |
| `@EncryptField`                             | Auto encrypt/decrypt DB fields           |
| `@DataMask`                                 | Response data masking                    |
| `@UpdateField`                              | Mark updateable fields in request DTOs   |
| `@UniqueField` / `@UniqueDetections`        | Unique constraint validation             |
| `@DynamicDictValue`                         | Validate field against dictionary values |
| `@StrixJob`                                 | Quartz job scheduling                    |

## Configuration

- Profiles: `dev`, `prod`, `test` via `application-{profile}.yml`
- Custom config prefix: `strix.*` (module toggles, captcha, rate-limit, delayed-task)
- Modules: `strix.module.sms/oss/oauth/pay/job/push: true/false`
- i18n: `resources/i18n/strix/`, default locale `zh_CN`
- Jackson: GMT+8, null exclusion, camelCase
- MyBatis Plus: snake_case → camelCase, soft delete on `deleted_status`