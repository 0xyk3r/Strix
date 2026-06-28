package cn.projectan.strix.model.response.system.tool.document;

import cn.projectan.strix.model.enums.DocumentConvertType;
import lombok.Data;

/**
 * 文档转换类型信息响应
 *
 * @author ProjectAn
 * @since 2026/6/28
 */
@Data
public class DocumentConvertTypeResp {

    /**
     * 枚举 code，用于提交时的 type 参数
     */
    private String code;

    /**
     * 展示名称
     */
    private String displayName;

    /**
     * 支持的源文件扩展名（逗号分隔）
     */
    private String sourceExtensions;

    /**
     * 目标文件扩展名
     */
    private String targetExtension;

    public DocumentConvertTypeResp(DocumentConvertType type) {
        this.code = type.name();
        this.displayName = type.getDisplayName();
        this.sourceExtensions = type.getSourceExtensions();
        this.targetExtension = type.getTargetExtension();
    }

}
