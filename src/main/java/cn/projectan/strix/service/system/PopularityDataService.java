package cn.projectan.strix.service.system;

import cn.projectan.strix.mapper.system.PopularityDataMapper;
import cn.projectan.strix.model.db.system.PopularityData;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * <p>
 * Strix 热度工具 数据 服务类
 * </p>
 *
 * @author ProjectAn
 * @since 2023-09-15
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PopularityDataService extends ServiceImpl<PopularityDataMapper, PopularityData> {

}
