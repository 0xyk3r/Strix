package cn.projectan.strix.model.other.monitor.cache;


/**
 * 缓存信息
 */
public class SystemCache {
    /**
     * 缓存名称
     */
    private String cacheName = "";

    /**
     * 缓存键名
     */
    private String cacheKey = "";

    /**
     * 缓存内容
     */
    private String cacheValue = "";

    /**
     * 备注
     */
    private String remark = "";

    public SystemCache() {

    }

    public SystemCache(String cacheName, String remark) {
        this.cacheName = cacheName;
        this.remark = remark;
    }

    public SystemCache(String cacheName, String cacheKey, String cacheValue) {
        this.cacheName = cacheName.replace(":", "");
        this.cacheKey = cacheKey.replace(cacheName, "");
        this.cacheValue = cacheValue;
    }

    public String getCacheName() {
        return cacheName;
    }

    public void setCacheName(String cacheName) {
        this.cacheName = cacheName;
    }

    public String getCacheKey() {
        return cacheKey;
    }

    public void setCacheKey(String cacheKey) {
        this.cacheKey = cacheKey;
    }

    public String getCacheValue() {
        return cacheValue;
    }

    public void setCacheValue(String cacheValue) {
        this.cacheValue = cacheValue;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }
}
