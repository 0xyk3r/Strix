package cn.projectan.strix.service;

import cn.projectan.strix.model.db.SystemDict;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * <p>
 * 服务类
 * </p>
 *
 * @author 安炯奕
 * @since 2021-08-31
 */
public interface SystemDictService extends IService<SystemDict> {

    /**
     * 获取字典值
     *
     * @param key 字典key
     * @return 字典值
     */
    String getDict(String key);

    /**
     * 保存字典纸
     *
     * @param key   字典key
     * @param value 字典值
     */
    void putDict(String key, String value);

    /**
     * 保存字典纸
     *
     * @param key      字典key
     * @param value    字典值
     * @param updateBy 数据修改人
     */
    void putDict(String key, String value, String updateBy);

}
