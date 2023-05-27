package cn.projectan.strix.model.request.base;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author 安炯奕
 * @date 2023/5/23
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BasePageReq<T> {

    private Integer pageSize = 10;

    private Integer pageIndex = 1;

    public Page<T> getPage() {
        return new Page<>(pageIndex, pageSize);
    }

}
