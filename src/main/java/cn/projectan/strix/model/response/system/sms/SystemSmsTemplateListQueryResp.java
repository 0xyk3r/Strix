package cn.projectan.strix.model.response.system.sms;

import cn.projectan.strix.model.db.SmsTemplate;
import cn.projectan.strix.model.response.base.BasePageResp;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author 安炯奕
 * @date 2023/5/20 19:31
 */
@Getter
@NoArgsConstructor
public class SystemSmsTemplateListQueryResp extends BasePageResp {

    private List<SmsTemplateItem> templates = new ArrayList<>();

    public SystemSmsTemplateListQueryResp(List<SmsTemplate> data, Long total) {
        templates = data.stream().map(d -> new SmsTemplateItem(d.getId(), d.getConfigId(), d.getCode(), d.getName(), d.getType(), d.getStatus(), d.getContent(), d.getCreateTime())).collect(Collectors.toList());
        this.setTotal(total);
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SmsTemplateItem {

        private String id;

        private String configId;

        private String code;

        private String name;

        private Integer type;

        private Integer status;

        private String content;

        private LocalDateTime createTime;

    }
}
