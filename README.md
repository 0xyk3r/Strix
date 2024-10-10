<h1 align="center">Strix</h1>
<p align="center">一个基于 Java 和 Spring Boot 的业务中台应用框架</p>

## 一、项目特性

### 1.1 使用 Java 21 & Spring Boot 3

Strix 使用最新的 Java 21 和 Spring Boot 3, 享受最新的语言特性和框架特性.

### 1.2 支持多种功能模块

Strix 支持可开关可配置的功能模块, Strix 提供了 SMS / OSS / Auth / Push / Job / Pay / Log / Captcha 等模块,
可以根据需要选择性的启用或禁用某些模块.

### 1.3 提供丰富的工具类

Strix 提供了多种工具类, 包括但不限于常用的参数处理工具 / 便捷开发工具 / 网络请求工具 / 常见数据算法 / 基于反射的快速编码工具等.

### 1.4 具备高度可扩展性

Strix 从设计之初就考虑到了可扩展性, 例如 SMS / OSS 等模块, 可以根据需求对多种不同的服务提供平台进行对接兼容.

### 1.5 提供国际化支持

Strix 提供了国际化支持, 可以根据需要对多种语言进行支持, 例如中文 / 英文等.

## 二、安装使用

### 2.1 打包构建

Strix 使用 Gradle 进行构建, 执行 `./gradlew build` 即可构建项目.

### 2.2 引入依赖

在你的项目中引入构建后得到的 `Strix-plain.jar` 包.

### 2.3 配置文件

基于 Spring Boot 配置文件, Strix 提供了多种配置项, 参考:

```yaml
strix:
  # 模块开关
  module:
    sms: true # 短信模块
    oss: true # 对象存储模块
    job: true # 定时任务模块
    oauth: true # 认证模块
    push: true # 推送模块
    pay: true # 支付模块
  # 开发环境下控制台输出原始请求内容
  show-request: false
  # 开发环境下控制台输出原始响应内容
  show-response: false
  # 安全模块配置
  security:
    jwt:
      secret-key: T3GMtWkpWMgioX4nyY1tnU9feVpRElw1PA8DRNpEmng=
      expire-time: 86400000
      refresh-expire-time: 604800000
  # 日志配置
  log:
    enable: false
  # 验证码配置
  captcha:
    type: blockPuzzle
    cache-type: redis
    aes-status: true
    water-mark: Strix
    interference-options: 1
    req-frequency-limit-enable: true
    req-get-lock-limit: 5
    req-get-lock-seconds: 300
    req-get-minute-limit: 20
  # 验证码计次器配置
  verifier-counter:
    sms:
      limit: 5
      seconds: 300
    email:
      limit: 5
      seconds: 300
  # 国际化配置
  default-locale: zh_CN
  # 包扫描路径
  package-scan:
    job:
    model:
```

## 三、详细使用说明

待添加......
