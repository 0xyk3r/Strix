package cn.projectan.strix.mapper;

import cn.projectan.strix.model.db.OauthUser;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * <p>
 * Strix OAuth 第三方用户信息 Mapper 接口
 * </p>
 *
 * @author ProjectAn
 * @since 2024-04-08
 */
@Mapper
public interface OauthUserMapper extends BaseMapper<OauthUser> {

}
