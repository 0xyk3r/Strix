package cn.projectan.strix.model.response.srv.chat;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Schema(description = "聊天消息列表响应")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessageListResp {

    @Schema(description = "消息列表")
    private List<ChatMessageResp> items;
}
