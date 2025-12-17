package cn.projectan.strix.model.request.base;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author ProjectAn
 * @since 2023/5/23
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BasePageReq<T> {

    @Schema(description = "分页大小", example = "10")
    private Integer pageSize = 10;

    @Schema(description = "分页页码", example = "1")
    private Integer pageIndex = 1;

    @JsonIgnore
    public Page<T> getPage() {
        return new Page<>(pageIndex, pageSize);
    }

}
