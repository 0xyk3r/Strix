# Strix 后端

本文件为 Claude Code 提供 Strix 后端项目的详细技术指导。

> **注意**：本文件是 `StrixProjects/CLAUDE.md` 的补充文档。在根目录工作时会自动加载此文件。

## 项目信息

- **版本**：3.0.0
- **Java 版本**：21（启用虚拟线程）
- **Spring Boot**：4.0.2
- **包名**：`cn.projectan.strix`
- **端口**：9889

## 构建与测试

```bash
# 在 Strix/ 目录执行
./gradlew build              # 构建（含测试）
./gradlew build -x test      # 构建（跳过测试）
./gradlew test               # 运行全部测试
./gradlew test --tests "cn.projectan.strix.某TestClass"  # 运行单个测试
./gradlew bootRun            # 启动开发服务器
./gradlew bootJar            # 生成可执行 JAR
./gradlew nativeCompile      # GraalVM 原生镜像编译
```

**测试框架**：JUnit 5 + Spring Boot Test + Spring Security Test

## 架构分层

**Controller → Service → Mapper → Database**

```
src/main/java/cn/projectan/strix/
├── controller/          # REST 接口层
│   ├── BaseController.java         # 基类（GraalVM 要求，所有 Controller 必须继承）
│   ├── system/                      # 系统管理接口
│   │   └── base/BaseSystemController.java
│   ├── srv/                         # 客户端业务接口
│   └── pay/                         # 支付回调接口
├── service/             # 业务逻辑层
│   ├── base/                        # 基础服务
│   ├── common/                      # 通用服务
│   └── system/                      # 系统管理服务
├── mapper/              # 数据访问层（MyBatis Plus）
│   └── system/
├── model/               # 数据模型
│   ├── db/                          # 数据库实体
│   ├── request/                     # 请求 DTO
│   ├── response/                    # 响应 DTO
│   ├── dict/                        # 数据字典枚举
│   ├── constant/                    # 常量
│   ├── annotation/                  # 自定义注解
│   ├── enums/                       # 枚举
│   └── properties/                  # 配置属性类
├── core/                # 框架核心
│   ├── ret/                         # 统一响应（RetResult/RetCode/RetBuilder）
│   ├── ss/                          # Spring Security 集成
│   ├── module/                      # 可插拔模块（oauth/oss/pay/sms/job）
│   ├── encrypt/                     # 字段加密（@EncryptField）
│   ├── datamask/                    # 数据脱敏（@DataMask）
│   ├── ratelimit/                   # 接口限流（@RateLimit）
│   ├── xss/                         # XSS 防护
│   ├── captcha/                     # 滑块验证码
│   ├── aop/                         # 切面与拦截器
│   └── validation/                  # 自定义校验器
├── util/                # 工具类
│   ├── algo/                        # 算法工具
│   ├── async/                       # 异步工具
│   ├── crypto/                      # 加密工具（SM2/SM3/SM4）
│   ├── common/                      # 通用工具（I18nUtil/UpdateBuilder/UniqueChecker）
│   └── ...
├── aot/                 # GraalVM AOT 配置
│   ├── BouncyCastleFeature.java
│   └── LambdaRegistrationFeature.java
└── config/              # Spring 配置
    ├── SecurityConfig.java
    ├── RedisConfig.java
    ├── MybatisPlusConfig.java
    └── ...
```

## Controller 模式

### 基本结构

```java
@RestController
@RequestMapping("system/user")
@RequiredArgsConstructor
@Tag(name = "系统 - 用户管理")
public class SystemUserController extends BaseSystemController {  // 必须继承
    
    private final SystemUserService systemUserService;
    
    @Operation(summary = "用户列表")
    @GetMapping("")
    @PreAuthorize("@ss.hasPermission('system:user')")  // 权限校验
    @StrixLog(operationGroup = "系统用户", operationName = "查询用户列表")  // 审计日志
    public RetResult<SystemUserListResp> list(SystemUserListReq req) {
        Page<SystemUser> page = systemUserService.listPage(req);
        return RetBuilder.success(new SystemUserListResp(page.getRecords(), page.getTotal()));
    }
    
    @Operation(summary = "新增用户")
    @PostMapping("update")
    @PreAuthorize("@ss.hasPermission('system:user:add')")
    @StrixLog(operationGroup = "系统用户", operationName = "新增用户", operationType = SystemLogOperType.ADD)
    public RetResult<Void> create(@RequestBody @Validated(InsertGroup.class) SystemUserUpdateReq req) {
        SystemUser systemUser = new SystemUser(/* ... */);
        UniqueChecker.check(systemUser);  // 唯一性校验
        systemUserService.save(systemUser);
        return RetBuilder.success();
    }
    
    @Operation(summary = "编辑用户")
    @PostMapping("update/{userId}")
    @PreAuthorize("@ss.hasPermission('system:user:update')")
    @StrixLog(operationGroup = "系统用户", operationName = "修改用户", operationType = SystemLogOperType.UPDATE)
    public RetResult<Void> update(@PathVariable String userId, 
                                   @RequestBody @Validated(UpdateGroup.class) SystemUserUpdateReq req) {
        SystemUser systemUser = systemUserService.getById(userId);
        Assert.notNull(systemUser, I18nUtil.notFound("field.systemUser"));
        
        LambdaUpdateWrapper<SystemUser> wrapper = UpdateBuilder.build(systemUser, req);  // 动态构建更新
        UniqueChecker.check(systemUser);
        systemUserService.update(wrapper);
        return RetBuilder.success();
    }
}
```

### 关键约定

1. **必须继承 `BaseController`**（直接或间接），否则 GraalVM 原生编译失败
2. **所有接口返回 `RetResult<T>`**，HTTP 状态码与业务码语义一致（200=成功，400=参数错误，401=未登录，403=无权限，404=未找到，500=服务器错误）
3. **权限校验**：`@PreAuthorize("@ss.hasPermission('module:resource:action')")`
4. **免认证接口**：使用 `@Anonymous` 注解
5. **审计日志**：`@StrixLog(operationGroup, operationName, operationType)`
6. **依赖注入**：使用 `@RequiredArgsConstructor` + final 字段

## Service 模式

```java
@Service
@RequiredArgsConstructor
public class SystemUserService extends ServiceImpl<SystemUserMapper, SystemUser> {
    
    private final SystemRoleService systemRoleService;
    
    /**
     * 分页查询
     */
    public Page<SystemUser> listPage(SystemUserListReq req) {
        return lambdaQuery()
                .like(StringUtils.hasText(req.getKeyword()), SystemUser::getNickname, req.getKeyword())
                .eq(NumUtil.checkCategory(req.getStatus(), NumCategory.NON_NEGATIVE), 
                    SystemUser::getStatus, req.getStatus())
                .orderByAsc(SystemUser::getCreatedTime)
                .page(req.getPage());
    }
    
    /**
     * 带关联删除的事务操作
     */
    @Transactional(rollbackFor = Exception.class)
    @Caching(evict = {
        @CacheEvict(value = "strix:user", key = "#user.id"),
        @CacheEvict(value = "strix:permission", key = "#user.id")
    })
    public void deleteUserWithRelations(SystemUser user) {
        // 删除用户
        removeById(user);
        // 删除用户角色关联
        systemRoleService.removeUserRoles(user.getId());
        // 清除会话
        // ...
    }
}
```

### 关键约定

1. **继承 `ServiceImpl<Mapper, Entity>`**，获得 MyBatis Plus 内置方法
2. **优先使用 `lambdaQuery()` / `lambdaUpdate()`**，减少自定义 XML
3. **事务控制**：`@Transactional(rollbackFor = Exception.class)`
4. **缓存注解**：`@Cacheable` / `@CacheEvict` / `@Caching`

## DTO 模式

### 请求 DTO

```java
// 列表请求 - 继承 BasePageReq 获得分页能力
@Data
public class SystemUserListReq extends BasePageReq<SystemUser> {
    @Size(max = 64, message = "{validation.size:field.keyword}")
    private String keyword;
    
    @DynamicDictValue(dictName = "SystemUserStatus")
    private Short status;
}

// 创建/更新请求 - 共用一个 DTO，分组校验
@Data
public class SystemUserUpdateReq {
    @NotEmpty(groups = {InsertGroup.class, UpdateGroup.class},
              message = "{validation.required:field.user.nickname}")
    @Size(groups = {InsertGroup.class, UpdateGroup.class}, min = 2, max = 16)
    @UpdateField  // 标记可更新字段
    private String nickname;
    
    @NotNull(groups = InsertGroup.class, message = "{validation.required:field.user.status}")
    @DynamicDictValue(groups = {InsertGroup.class, UpdateGroup.class}, dictName = "SystemUserStatus")
    @UpdateField
    private Short status;
    
    @Pattern(groups = {InsertGroup.class, UpdateGroup.class}, 
             regexp = "^1[3-9]\\d{9}$", message = "{validation.pattern:field.phoneNumber}")
    @UniqueField(entity = SystemUser.class, field = "phoneNumber")  // 唯一性约束
    @UpdateField
    private String phoneNumber;
}
```

### 响应 DTO

```java
@Data
public class SystemUserResp {
    private String id;
    private String nickname;
    private Short status;
    private String phoneNumber;
    private String createdTime;
    
    // 从实体构造
    public SystemUserResp(SystemUser user) {
        this.id = user.getId();
        this.nickname = user.getNickname();
        this.status = user.getStatus();
        this.phoneNumber = user.getPhoneNumber();
        this.createdTime = user.getCreatedTime();
    }
}

@Data
public class SystemUserListResp {
    private List<SystemUserResp> systemUserList;
    private Long total;
    
    public SystemUserListResp(List<SystemUser> records, Long total) {
        this.systemUserList = records.stream().map(SystemUserResp::new).toList();
        this.total = total;
    }
}
```

## 实体（Entity）模式

```java
@Data
@TableName("system_user")
@EqualsAndHashCode(callSuper = true)
public class SystemUser extends BaseModel<SystemUser> {
    
    @TableId(type = IdType.ASSIGN_ID)  // 雪花 ID
    private String id;
    
    private String nickname;
    
    private Short status;
    
    @UniqueDetection(message = "{validation.unique:field.phoneNumber}")
    private String phoneNumber;
    
    @EncryptField  // 字段加密
    private String sensitiveData;
    
    @TableLogic  // 逻辑删除（0=正常，1=已删除）
    private Short deletedStatus;
    
    // 审计字段（自动填充）
    private String createdTime;
    private String createdBy;
    private Short createdByType;
    private String updatedTime;
    private String updatedBy;
    private Short updatedByType;
}
```

### 关键约定

1. **继承 `BaseModel<T>`**，获得审计字段和 Fluent Setter
2. **主键策略**：`@TableId(type = IdType.ASSIGN_ID)`（雪花 ID）
3. **逻辑删除**：`@TableLogic` 标记 `deletedStatus` 字段
4. **唯一校验**：`@UniqueDetection` 标记需要唯一性检查的字段
5. **字段加密**：`@EncryptField` 自动加密/解密敏感字段

## 核心工具类

### UpdateBuilder

从 DTO 自动构建更新条件：

```java
LambdaUpdateWrapper<SystemUser> wrapper = UpdateBuilder.build(entity, req);
// 只更新标记了 @UpdateField 的字段
```

### UniqueChecker

唯一性校验：

```java
UniqueChecker.check(systemUser);  // 检查 @UniqueDetection 字段
// 抛出 StrixUniqueCheckerException 如果重复
```

### I18nUtil

国际化工具：

```java
String msg = I18nUtil.get("error.param.invalid");
String notFound = I18nUtil.notFound("field.systemUser");  // "系统用户不存在"
```

### RetBuilder

统一响应构建：

```java
return RetBuilder.success();                    // { code: 200, msg: "success", data: null }
return RetBuilder.

success(data);                // { code: 200, msg: "success", data: {...} }
return RetBuilder.

error("错误信息");             // { code: 400, msg: "错误信息", data: null } — 业务参数错误
return RetBuilder.

serverError("服务器异常");     // { code: 500, msg: "服务器异常", data: null } — 系统异常
return RetBuilder.

build(RetCode.NOT_FOUND, msg); // 使用预定义错误码
```

## 配置

### application.yml 结构

```yaml
# 激活的 Profile
spring:
  profiles:
    active: dev

# Strix 配置前缀
strix:
  module:                    # 模块开关
    sms: true
    oss: true
    oauth: false
    pay: true
    job: true
  captcha:                   # 验证码配置
    enabled: true
  rate-limit:                # 限流配置
    default-limit: 600
    default-period: 60
  cors:                      # CORS 配置
    allowed-origins: 
      - http://localhost:13232
```

### Profile 配置

- `application-dev.yml` — 开发环境（端口 9889）
- `application-test.yml` — 测试环境
- `application-prod.yml` — 生产环境

## 常用注解

| 注解                  | 用途    | 示例                                                         |
|---------------------|-------|------------------------------------------------------------|
| `@Anonymous`        | 跳过认证  | 登录、验证码、支付回调                                                |
| `@PreAuthorize`     | 权限校验  | `@PreAuthorize("@ss.hasPermission('system:user')")`        |
| `@StrixLog`         | 审计日志  | `@StrixLog(operationGroup = "系统用户", operationName = "新增")` |
| `@RateLimit`        | 接口限流  | `@RateLimit(limit = 10, period = 60)`                      |
| `@EncryptField`     | 字段加密  | 敏感数据自动加密/解密                                                |
| `@DataMask`         | 数据脱敏  | 响应中脱敏（手机号、身份证等）                                            |
| `@UpdateField`      | 可更新字段 | 配合 `UpdateBuilder` 使用                                      |
| `@UniqueDetection`  | 唯一性约束 | 配合 `UniqueChecker` 使用                                      |
| `@DynamicDictValue` | 字典校验  | 验证值是否在字典中                                                  |

## 模块化

### 可插拔模块

通过 `strix.module.*` 配置启用/禁用：

- **SMS**：短信发送（阿里云、腾讯云）
- **OSS**：对象存储（阿里云 OSS、本地存储）
- **OAuth**：第三方登录（微信、QQ、GitHub）
- **Pay**：支付集成（支付宝、微信支付）
- **Job**：定时任务（Quartz）
- **Push**：消息推送

### 模块启用方式

```yaml
strix:
  module:
    sms: true      # 启用短信模块
    oss: true      # 启用对象存储模块
    pay: false     # 禁用支付模块
```

## GraalVM 原生编译

### 要求

1. **所有 Controller 必须继承 `BaseController`**
2. AOT 配置已预置：`src/main/java/cn/projectan/strix/aot/`
3. 资源文件配置：`src/main/resources/META-INF/native-image/`

### 构建命令

```bash
./gradlew nativeCompile        # 编译原生镜像
./gradlew bootBuildImage       # 构建 Docker 原生镜像
```

## 注意事项

### Java 21 虚拟线程

- 已在 `application.yml` 中启用
- **避免使用 `ThreadLocal`**，改用 `ScopedValue`
- 兼容所有 Spring 异步操作

### MyBatis Plus

- **优先使用 Lambda 方式**：`lambdaQuery()` / `lambdaUpdate()`
- **减少 XML 使用**：仅在复杂查询时使用
- **分页插件**：已配置，直接使用 `Page<T>`

### Redis 缓存

- **缓存 Key 规范**：`strix:模块:业务:id`
- **过期时间**：根据业务设定
- **缓存清理**：使用 `@CacheEvict` 自动清理

### 国际化

- **消息格式**：`{key:placeholder}` → `I18nUtil.get("key", "placeholder")`
- **资源文件位置**：`src/main/resources/i18n/strix/`
- **默认语言**：zh_CN

---

**提示**：本文档是技术实现细节，更高层次的项目架构和工作流请参考根目录的 `CLAUDE.md`。
