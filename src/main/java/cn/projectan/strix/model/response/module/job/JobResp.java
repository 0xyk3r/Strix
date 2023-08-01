package cn.projectan.strix.model.response.module.job;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author 安炯奕
 * @date 2023/7/30 17:16
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class JobResp {

    private String id;

    private String name;

    private String group;

    private String invokeTarget;

    private String cronExpression;

    private Integer misfirePolicy;

    private Integer concurrent;

    private Integer status;

}
