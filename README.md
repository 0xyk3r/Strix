<h1 align="center">Strix</h1>
<p align="center">一个基于 Java 21 / Spring Boot 4 的业务中台框架</p>

---

## 项目概览（Project Overview）

Strix 是一个基于 **Java 21 / Spring Boot 4** 的业务中台框架（Business Middleware Framework）。

它提供了可插拔的功能模块，包括：**短信（SMS）**、**对象存储（OSS）**、**授权登录（OAuth）**、**支付（Pay）**、**任务调度（Job）**、*
*验证码（Captcha）** 以及 **延迟任务（DelayedTask）** 处理能力。

---

## 构建命令（Build Commands）

```bash
# 构建（包含测试）
./gradlew build

# 构建（跳过测试）
./gradlew build -x test

# 生成可执行 JAR
./gradlew bootJar

# 运行测试
./gradlew test

# 运行单个测试类
./gradlew test --tests "cn.projectan.strix.SomeTestClass"

# 生成 GraalVM 原生镜像
./gradlew nativeCompile

# 构建支持原生镜像的 Docker 镜像
./gradlew bootBuildImage
```

---

## 架构设计（Architecture）

### 包结构（`cn.projectan.strix`）

* **controller/** - 按业务域划分的 REST 接口层（api / system / pay / wechat 等）
* **service/** - 业务逻辑层（base / common / system）
* **mapper/** - MyBatis DAO 接口
* **model/** - 数据模型

  * `db/` - 数据库实体
  * `request/` - 请求 DTO
  * `response/` - 响应 DTO
  * `dict/` - 字典模型
  * `constant/` - 常量定义
  * `annotation/` - 自定义注解
  * `enums/` - 枚举类
  * `properties/` - 配置属性类
* **core/** - 框架核心模块

  * `ret/` - 统一响应模型（RetResult、RetCode、RetBuilder）
  * `ss/` - Spring Security 集成
  * `encrypt/` - 字段级加密体系
  * `datamask/` - 数据脱敏
  * `module/` - 可插拔模块（OAuth、OSS、Pay、SMS 等）
  * `validation/` - 自定义校验器
  * `aop/` - 切面与拦截器
* **util/** - 工具类，按功能分类（algo / async / encrypt / http 等）
* **aot/** - GraalVM AOT 编译相关支持
* **config/** - Spring 配置类

---

### 核心设计模式（Key Patterns）

**统一响应格式**
所有 API 接口统一返回 `RetResult<T>`，
通过 `RetCode` 枚举定义状态码，并使用 `RetBuilder` 构建响应对象。

**安全机制**
采用无状态 Token 认证机制，支持两类用户：

* `SystemManager`
* `SystemUser`

已关闭 CSRF，开启 CORS 支持。

**数据访问层**
基于 **MyBatis Plus**，并内置以下能力：

* 自动字段加密（`@EncryptField`）
* 数据脱敏
* 逻辑删除
* 审计字段（createdBy / updatedBy / createdTime / updatedTime）
* 乐观锁支持

**模块化设计**
通过 `strix.module.*` 配置项按需启用或禁用模块：
`sms`、`oss`、`job`、`oauth`、`push`、`pay`

---

### 技术栈（Tech Stack）

* Spring Boot 4，Java 21
* MyBatis Plus 3.5.16（MySQL 8+）
* Redis / Redisson（缓存）
* Quartz（任务调度）
* AWS SDK v2（S3 对象存储）
* 支付宝 / 微信支付 SDK

---

## 代码生成（Code Generation）

使用 `MysqlGenerator.java`，可根据数据库表结构自动生成：

* Entity
* Mapper
* Service 等基础代码

---

## 配置说明（Configuration）

环境配置文件：

```
application-{dev,prod,test}.yml
```

核心配置前缀：`strix.*`

* `module.*` - 模块启用 / 禁用配置
* `captcha.*` - 验证码相关配置
* `delayed-task.*` - 异步与延迟任务处理
* `package-scan.job/model` - 动态类扫描配置
* `default-locale` - 国际化配置（默认：`zh_CN`）
