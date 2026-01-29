package cn.projectan.strix.model.other.system.module.sms;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * @author ProjectAn
 * @since 2023/5/20 18:33
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class StrixSmsTemplate {

    private String code;

    private String name;

    private Short type;

    private Short status;

    private String content;

    private LocalDateTime createdTime;

}
