package cn.projectan.strix.model.request.system.tool.popularity;

import cn.projectan.strix.core.validation.group.InsertGroup;
import cn.projectan.strix.core.validation.group.UpdateGroup;
import cn.projectan.strix.model.annotation.UpdateField;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;
import cn.projectan.strix.model.annotation.FormSchema;

/**
 * @author ProjectAn
 * @since 2023/10/5 21:48
 */
@Schema(description = "热度配置更新请求")
@FormSchema
@Data
public class PopularityConfigUpdateReq {

    @Schema(description = "配置名称")
    @NotEmpty(groups = {InsertGroup.class, UpdateGroup.class}, message = "{validation.required:field.popularity.configName}")
    @Size(groups = {InsertGroup.class, UpdateGroup.class}, min = 1, max = 32, message = "{validation.length:field.popularity.configName}")
    @UpdateField
    private String name;

    @Schema(description = "配置Key")
    @NotEmpty(groups = {InsertGroup.class, UpdateGroup.class}, message = "{validation.required:field.popularity.configKey}")
    @Size(groups = {InsertGroup.class, UpdateGroup.class}, min = 1, max = 32, message = "{validation.length:field.popularity.configKey}")
    @UpdateField
    private String configKey;

    @Schema(description = "初始值")
    @NotNull(groups = {InsertGroup.class, UpdateGroup.class}, message = "{validation.required:field.popularity.initValue}")
    @Min(groups = {InsertGroup.class, UpdateGroup.class}, value = 0, message = "{validation.outOfRange:field.popularity.initValue}")
    @UpdateField
    private Long initialValue;

    @Schema(description = "附加值")
    @NotNull(groups = {InsertGroup.class, UpdateGroup.class}, message = "{validation.required:field.popularity.addValue}")
    @Min(groups = {InsertGroup.class, UpdateGroup.class}, value = 0, message = "{validation.outOfRange:field.popularity.addValue}")
    @UpdateField
    private Long extraValue;

    @Schema(description = "倍率")
    @NotNull(groups = {InsertGroup.class, UpdateGroup.class}, message = "{validation.required:field.popularity.multiplier}")
    @DecimalMin(groups = {InsertGroup.class, UpdateGroup.class}, value = "0.01", message = "{validation.outOfRange:field.popularity.multiplier}")
    @UpdateField
    private BigDecimal magValue;

}
