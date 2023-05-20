package cn.projectan.strix.model.system;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * @author 安炯奕
 * @date 2023/5/20 18:33
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class StrixSmsTemplate {

    private String code;

    private String name;

    private Integer type;

    private Integer status;

    private String content;

    private LocalDateTime createTime;

}
