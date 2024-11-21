package cn.projectan.strix.service;

import com.baomidou.mybatisplus.extension.service.IService;

/**
 * 数据 ID 映射器
 *
 * @author ProjectAn
 * @since 2024-11-18 17:33:50
 */
public interface NameFetcherService<T> extends IService<T> {

    String getDataNameById(String id);

}
