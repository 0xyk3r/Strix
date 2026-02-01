package cn.projectan.strix.mapper.system;

import cn.projectan.strix.model.db.system.ChatSessionMember;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * <p>
 * Strix Chat 会话成员 Mapper 接口
 * </p>
 *
 * @author ProjectAn
 * @since 2026-02-01
 */
@Mapper
public interface ChatSessionMemberMapper extends BaseMapper<ChatSessionMember> {

}
