package cn.projectan.strix.model.request.common;

import lombok.Data;

/**
 * 单一属性修改请求参数
 *
 * @author 安炯奕
 * @date 2021/6/16 15:18
 */
@Data
public class SingleFieldModifyReq {

    private String field;

    private String value;

}
