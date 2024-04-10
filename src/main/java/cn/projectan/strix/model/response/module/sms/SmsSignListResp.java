package cn.projectan.strix.model.response.module.sms;

import cn.projectan.strix.model.db.SmsSign;
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
 * @author ProjectAn
 * @date 2023/5/20 19:27
 */
@Getter
@NoArgsConstructor
public class SmsSignListResp extends BasePageResp {

    private List<SmsSignItem> signs = new ArrayList<>();

    public SmsSignListResp(List<SmsSign> data, Long total) {
        signs = data.stream().map(d ->
                new SmsSignItem(d.getId(), d.getConfigKey(), d.getName(), d.getStatus(), d.getCreateTime())
        ).collect(Collectors.toList());
        this.setTotal(total);
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SmsSignItem {

        private String id;

        private String configKey;

        private String name;

        private Integer status;

        private LocalDateTime createTime;

    }

}
