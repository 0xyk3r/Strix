package cn.projectan.strix.model.db.system;

import cn.projectan.strix.model.db.base.BaseModel;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.io.Serial;
import java.time.LocalDateTime;

/**
 * 文档 AI 分析任务
 * <p>
 * 持久化任务元数据与分析结果，使任务在服务重启 / 客户端刷新后仍可查询最终结果并断线续播。
 * 页面图片存磁盘临时目录（不入库、不占堆），随任务过期清理。
 *
 * @author ProjectAn
 * @since 2026-07-04
 */
@Getter
@Setter
@Accessors(chain = true)
@TableName("sys_ai_document_task")
public class AiDocumentTask extends BaseModel<AiDocumentTask> {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 归属管理员 ID（越权校验用）
     */
    private String managerId;

    /**
     * 任务状态：PROCESSING / DONE / FAILED
     */
    private String status;

    /**
     * 分析提示词
     */
    private String prompt;

    /**
     * 视觉模型配置 Key（DOC 输入）或分析模型 Key（TEXT 输入）
     */
    private String visionModelKey;

    /**
     * 合并 / 文本模型配置 Key
     */
    private String textModelKey;

    /**
     * 是否启用合并：0=否 1=是
     */
    private Integer mergeEnabled;

    /**
     * 原始文件名
     */
    private String fileName;

    /**
     * 输入类型：DOC（图片批次）/ TEXT（纯文本）
     */
    private String inputType;

    /**
     * 总页数（TEXT 为 0）
     */
    private Integer totalPages;

    /**
     * 总批次数
     */
    private Integer totalBatches;

    /**
     * 各批次页码范围描述 JSON（List&lt;String&gt;）
     */
    private String batchDescriptions;

    /**
     * 各批次结果 JSON（{"0":"...","1":"..."}）
     */
    private String batchResults;

    /**
     * 合并结果文本
     */
    private String mergeResult;

    /**
     * 错误信息（FAILED 时填充）
     */
    private String errorMessage;

    /**
     * 过期时间（到期清理图片与记录）
     */
    private LocalDateTime expireAt;

}
