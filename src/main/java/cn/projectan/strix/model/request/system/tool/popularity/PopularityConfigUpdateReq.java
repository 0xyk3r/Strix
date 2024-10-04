package cn.projectan.strix.model.request.system.tool.popularity;

import cn.projectan.strix.core.validation.group.InsertGroup;
import cn.projectan.strix.core.validation.group.UpdateGroup;
import cn.projectan.strix.model.annotation.UpdateField;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;

/**
 * @author ProjectAn
 * @since 2023/10/5 21:48
 */
@Data
public class PopularityConfigUpdateReq {

    @NotEmpty(groups = {InsertGroup.class, UpdateGroup.class}, message = " 配置名称不可为空")
    @Size(groups = {InsertGroup.class, UpdateGroup.class}, min = 1, max = 32, message = "配置名称长度不符合要求")
    @UpdateField
    private String name;

    @NotEmpty(groups = {InsertGroup.class, UpdateGroup.class}, message = " 配置Key不可为空")
    @Size(groups = {InsertGroup.class, UpdateGroup.class}, min = 1, max = 32, message = "配置Key长度不符合要求")
    @UpdateField
    private String configKey;

    @NotNull(groups = {InsertGroup.class, UpdateGroup.class}, message = " 初始值不可为空")
    @Min(groups = {InsertGroup.class, UpdateGroup.class}, value = 0, message = "初始值超出范围")
    @UpdateField
    private Integer initialValue;

    @NotNull(groups = {InsertGroup.class, UpdateGroup.class}, message = " 附加值不可为空")
    @Min(groups = {InsertGroup.class, UpdateGroup.class}, value = 0, message = "附加值超出范围")
    @UpdateField
    private Integer extraValue;

    @NotNull(groups = {InsertGroup.class, UpdateGroup.class}, message = " 倍率不可为空")
    @DecimalMin(groups = {InsertGroup.class, UpdateGroup.class}, value = "0.01", message = "倍率超出范围")
    @UpdateField
    private BigDecimal magValue;

}
