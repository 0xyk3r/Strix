package cn.projectan.strix.model.response.module.sms;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * @author ProjectAn
 * @since 2023/5/20 19:19
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SmsConfigResp {

    private String id;

    private String key;

    private String name;

    private Integer platform;

    private String regionId;

    private String accessKey;

    private String remark;

    private LocalDateTime createdTime;

    private List<SmsSignListResp.SmsSignItem> signs;

    private List<SmsTemplateListResp.SmsTemplateItem> templates;

}
