package cn.projectan.strix.model.response.system.sms;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * @author 安炯奕
 * @date 2023/5/20 19:19
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SystemSmsConfigQueryByIdResp {

    private String id;

    private Integer platform;

    private String regionId;

    private String accessKey;

    private LocalDateTime createTime;

    private List<SystemSmsSignListQueryResp.SmsSignItem> signs;

    private List<SystemSmsTemplateListQueryResp.SmsTemplateItem> templates;

}
