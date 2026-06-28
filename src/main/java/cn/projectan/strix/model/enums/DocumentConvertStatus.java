package cn.projectan.strix.model.enums;

/**
 * 文档转换任务状态枚举
 *
 * @author ProjectAn
 * @since 2026/6/28
 */
public class DocumentConvertStatus {

    /**
     * 待处理（已提交，等待执行）
     */
    public static final String PENDING = "PENDING";

    /**
     * 处理中
     */
    public static final String PROCESSING = "PROCESSING";

    /**
     * 转换完成
     */
    public static final String COMPLETED = "COMPLETED";

    /**
     * 转换失败
     */
    public static final String FAILED = "FAILED";

}
