package cn.projectan.strix.mapper.system;

import cn.projectan.strix.model.db.system.ChatMessage;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * <p>
 * Strix Chat 消息 Mapper 接口
 * </p>
 *
 * @author ProjectAn
 * @since 2026-02-01
 */
@Mapper
public interface ChatMessageMapper extends BaseMapper<ChatMessage> {

}
