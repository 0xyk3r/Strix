package cn.projectan.strix.core.ret;

import java.util.List;

/**
 * 分页响应信息
 *
 * @author ProjectAn
 * @since 2021/1/31 18:22
 */
public class RetPageResult<T> extends RetResult<T> {

    private long total;
    private List<T> rows;

    public long getTotal() {
        return total;
    }

    public void setTotal(long total) {
        this.total = total;
    }

    public List<T> getRows() {
        return rows;
    }

    public void setRows(List<T> rows) {
        this.rows = rows;
    }

    public RetPageResult(int code, String msg, long total, List<T> rows) {
        super.setCode(code);
        this.setMsg(msg);
        this.total = total;
        this.rows = rows;
    }
}
