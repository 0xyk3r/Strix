package cn.projectan.strix.model.request.system.monitor.log;

import cn.projectan.strix.model.db.SystemLog;
import cn.projectan.strix.model.request.base.BasePageReq;
import lombok.Data;

/**
 * @author ProjectAn
 * @since 2023/6/17 22:27
 */
@Data
public class SystemLogListReq extends BasePageReq<SystemLog> {

    /**
     * 操作名称
     */
    private String keyword;

    /**
     * 操作类型
     */
    private String operationType;

    /**
     * 操作分组
     */
    private String operationGroup;

    /**
     * 响应状态码
     */
    private Integer responseCode;

}
