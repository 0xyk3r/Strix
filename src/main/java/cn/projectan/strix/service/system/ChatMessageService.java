package cn.projectan.strix.service.system;

import cn.projectan.strix.mapper.system.ChatMessageMapper;
import cn.projectan.strix.model.db.system.ChatMessage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * <p>
 * Strix Chat 消息 服务实现类
 * </p>
 *
 * @author ProjectAn
 * @since 2026-02-01
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ChatMessageService extends ServiceImpl<ChatMessageMapper, ChatMessage> {

}
