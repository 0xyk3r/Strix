package cn.projectan.strix.service.base;

import com.baomidou.mybatisplus.extension.service.IService;

/**
 * 数据 ID 映射器
 *
 * @author ProjectAn
 * @since 2024-11-18 17:33:50
 */
public interface NameFetcherService<T> extends IService<T> {

    /**
     * 根据 ID 获取名称
     *
     * @param id 数据 ID
     * @return 名称
     */
    String getDataNameById(String id);

}
