package cn.projectan.strix.mapper;

import cn.projectan.strix.model.db.SystemRegion;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * <p>
 * Mapper 接口
 * </p>
 *
 * @author ProjectAn
 * @since 2021-09-29
 */
@Mapper
public interface SystemRegionMapper extends BaseMapper<SystemRegion> {

    List<SystemRegion> getMatchChildren(@Param("parentFullName") String parentFullName);

}
