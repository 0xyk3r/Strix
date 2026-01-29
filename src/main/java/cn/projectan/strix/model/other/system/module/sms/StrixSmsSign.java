package cn.projectan.strix.model.other.system.module.sms;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * @author ProjectAn
 * @since 2023/5/20 17:28
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class StrixSmsSign {

    private String name;

    private Short status;

    private LocalDateTime createdTime;

}
