package cn.projectan.strix.model.request.base;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author ProjectAn
 * @since 2023/5/23
 */
@Schema(description = "分页基础请求")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BasePageReq<T> {

    @Min(value = 1, message = "{validation.minValue:field.pageSize}")
    @Max(value = 200, message = "{validation.maxValue:field.pageSize}")
    @Schema(description = "分页大小", example = "10")
    private Integer pageSize = 10;

    @Min(value = 1, message = "{validation.minValue:field.pageNumber}")
    @Schema(description = "分页页码", example = "1")
    private Integer pageIndex = 1;

    @JsonIgnore
    public Page<T> getPage() {
        return new Page<>(pageIndex, pageSize);
    }

}
