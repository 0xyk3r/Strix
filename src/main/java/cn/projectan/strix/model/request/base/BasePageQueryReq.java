package cn.projectan.strix.model.request.base;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author 安炯奕
 * @date 2021/6/11 17:56
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BasePageQueryReq<T> extends BaseReq {

    private Integer size = 10;

    private Integer current = 1;

    public Page<T> getPage() {
        Page<T> page = new Page<>();
        page.setSize(this.size);
        page.setCurrent(this.current);
        return page;
    }

}
