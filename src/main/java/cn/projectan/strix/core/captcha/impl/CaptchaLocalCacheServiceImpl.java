package cn.projectan.strix.core.captcha.impl;

import cn.projectan.strix.core.captcha.CaptchaCacheService;
import cn.projectan.strix.core.captcha.util.StrixCaptchaCacheUtil;

/**
 * 验证码缓存方案 (local 内存)
 *
 * @author ProjectAn
 * @since 2024/3/30 13:00
 */
public class CaptchaLocalCacheServiceImpl implements CaptchaCacheService {

    @Override
    public void set(String key, String value, long expiresInSeconds) {
        StrixCaptchaCacheUtil.set(key, value, expiresInSeconds);
    }

    @Override
    public boolean exists(String key) {
        return StrixCaptchaCacheUtil.exists(key);
    }

    @Override
    public void delete(String key) {
        StrixCaptchaCacheUtil.delete(key);
    }

    @Override
    public String get(String key) {
        return StrixCaptchaCacheUtil.get(key);
    }

    @Override
    public String type() {
        return "local";
    }

    @Override
    public Long increment(String key, long val) {
        String current = StrixCaptchaCacheUtil.get(key);
        long newVal = (current != null ? Long.parseLong(current) : 0) + val;
        // 保留原 TTL: 先读取是否存在, 再覆盖写入 (保持 60s 默认 TTL)
        StrixCaptchaCacheUtil.set(key, String.valueOf(newVal), 60);
        return newVal;
    }

}
