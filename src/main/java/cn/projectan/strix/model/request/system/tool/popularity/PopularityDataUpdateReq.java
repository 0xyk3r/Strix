package cn.projectan.strix.model.request.system.tool.popularity;

import cn.projectan.strix.core.validation.group.InsertGroup;
import cn.projectan.strix.core.validation.group.UpdateGroup;
import cn.projectan.strix.model.annotation.UpdateField;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * @author 安炯奕
 * @date 2023/10/6 11:25
 */
@Data
public class PopularityDataUpdateReq {

    @NotNull(groups = {InsertGroup.class, UpdateGroup.class}, message = "数值不可为空")
    @Min(groups = {InsertGroup.class, UpdateGroup.class}, value = 0, message = "数值超出范围")
    @UpdateField
    private Integer originalValue;

}
