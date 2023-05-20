package cn.projectan.strix.model.request.system.region;

import cn.projectan.strix.core.validation.ValidationGroup;
import cn.projectan.strix.model.annotation.UpdateField;
import cn.projectan.strix.model.request.base.BaseReq;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * @author 安炯奕
 * @date 2021/9/29 18:50
 */
@Data
public class SystemRegionUpdateReq extends BaseReq {

    @NotEmpty(groups = {ValidationGroup.Insert.class, ValidationGroup.Update.class}, message = "地区名称不可为空")
    @Size(groups = {ValidationGroup.Insert.class, ValidationGroup.Update.class}, min = 2, max = 32, message = "地区名称长度不符合要求")
    @UpdateField
    private String name;

    @UpdateField
    private String parentId;

    @Size(groups = {ValidationGroup.Insert.class, ValidationGroup.Update.class}, max = 128, message = "地区备注信息长度不符合要求")
    @UpdateField(allowEmpty = true)
    private String remarks;

}
