package cn.projectan.strix.mapper.system;

import cn.projectan.strix.model.db.system.AiMessage;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * AI 对话消息 Mapper
 *
 * @author ProjectAn
 * @since 2026-05-12
 */
@Mapper
public interface AiMessageMapper extends BaseMapper<AiMessage> {

}
