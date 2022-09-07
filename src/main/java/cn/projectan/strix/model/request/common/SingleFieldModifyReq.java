package cn.projectan.strix.model.request.common;

import cn.projectan.strix.model.request.base.BaseReq;
import lombok.Data;

/**
 * 单一属性修改请求参数
 *
 * @author 安炯奕
 * @date 2021/6/16 15:18
 */
@Data
public class SingleFieldModifyReq extends BaseReq {

    private String field;

    private String value;

}
