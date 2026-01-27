package cn.projectan.strix.service.system;

import cn.projectan.strix.mapper.system.DictDataMapper;
import cn.projectan.strix.model.db.system.DictData;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * <p>
 * Strix 字典数据 服务类
 * </p>
 *
 * @author ProjectAn
 * @since 2023-05-28
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DictDataService extends ServiceImpl<DictDataMapper, DictData> {

}
