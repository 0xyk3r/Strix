# 验证码API升级指南

本文档说明验证码模块API的变更内容，帮助前端开发者进行相应的代码调整。

## 变更概述

验证码API响应格式已简化，移除了嵌套的 `repCode/repData` 结构，统一使用项目标准的 `RetResult<T>` 格式。

## 响应格式变更

### 1. 获取验证码 (POST /system/captcha/get)

**旧响应格式:**

```json
{
  "code": 200,
  "msg": "success",
  "data": {
    "repCode": "0000",
    "repMsg": null,
    "repData": {
      "token": "uuid-xxx",
      "originalImageBase64": "...",
      "jigsawImageBase64": "...",
      "secretKey": "aes-key-xxx"
    }
  }
}
```

**新响应格式:**

```json
{
  "code": 200,
  "msg": "success",
  "data": {
    "token": "uuid-xxx",
    "originalImageBase64": "...",
    "jigsawImageBase64": "...",
    "secretKey": "aes-key-xxx"
  }
}
```

### 2. 校验验证码 (POST /system/captcha/check)

**旧响应格式:**

```json
{
  "code": 200,
  "msg": "success",
  "data": {
    "repCode": "0000",
    "repMsg": null,
    "repData": {
      "result": true,
      "captchaVerification": "encrypted-verification-token"
    }
  }
}
```

**新响应格式:**

```json
{
  "code": 200,
  "msg": "success",
  "data": {
    "result": true,
    "captchaVerification": "encrypted-verification-token"
  }
}
```

## 错误响应变更

### 旧错误响应格式

业务错误通过 `repCode` 字段返回，HTTP状态码始终为200：

```json
{
  "code": 200,
  "msg": "success",
  "data": {
    "repCode": "6110",
    "repMsg": "验证码已失效，请重新获取",
    "repData": null
  }
}
```

### 新错误响应格式

业务错误直接通过 `code` 字段返回：

```json
{
  "code": 400,
  "msg": "验证码已失效，请重新获取",
  "data": null
}
```

## 常见错误码

| 场景     | 旧 repCode | 新 code | 错误信息                 |
|--------|-----------|--------|----------------------|
| 验证码已过期 | 6110      | 400    | 验证码已失效，请重新获取         |
| 坐标校验失败 | 6111      | 400    | 验证码坐标错误              |
| 底图未初始化 | 6113      | 400    | 验证码底图未初始化            |
| 请求频率限制 | 6201-6205 | 400    | 验证码获取/校验请求过于频繁，请稍后再试 |

## 响应类型定义 (TypeScript)

```typescript
// 验证码获取响应
interface CaptchaGetResp {
    token: string;
    originalImageBase64: string;
    jigsawImageBase64: string;
    secretKey: string;
}

// 验证码校验响应
interface CaptchaCheckResp {
    result: boolean;
    captchaVerification: string;
}

// 统一响应格式
interface RetResult<T> {
    code: number;
    msg: string;
    data: T | null;
}

// API响应类型
type CaptchaGetResponse = RetResult<CaptchaGetResp>;
type CaptchaCheckResponse = RetResult<CaptchaCheckResp>;
```

## 升级检查清单

- [ ] 移除对 `repCode` 和 `repData` 的引用
- [ ] 将成功判断从 `repCode === '0000'` 改为 `code === 200`
- [ ] 将错误信息从 `repMsg` 改为 `msg`
- [ ] 将数据访问从 `data.repData.xxx` 改为 `data.xxx`
- [ ] 更新 TypeScript 类型定义

## 兼容性说明

此次变更为破坏性变更，前端必须同步更新。
