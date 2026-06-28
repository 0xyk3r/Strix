package cn.projectan.strix.model.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 文档格式转换类型枚举
 * <p>
 * 每个枚举值描述一种受支持的格式转换，包含展示名称和目标文件扩展名。
 * </p>
 *
 * @author ProjectAn
 * @since 2026/6/28
 */
@Getter
@RequiredArgsConstructor
public enum DocumentConvertType {

    // ===== Excel 相关 =====
    CELLS_TO_PDF("Excel 转 PDF", "xlsx,xls", "pdf"),
    CELLS_TO_HTML("Excel 转 HTML", "xlsx,xls", "html"),
    CELLS_TO_CSV("Excel 转 CSV", "xlsx,xls", "csv"),
    CELLS_TO_IMAGE("Excel 首张表转图片", "xlsx,xls", "png"),
    CELLS_TO_IMAGES("Excel 全部表转图片（ZIP）", "xlsx,xls", "zip"),

    // ===== PDF 相关 =====
    PDF_TO_WORD("PDF 转 Word", "pdf", "docx"),
    PDF_TO_EXCEL("PDF 转 Excel", "pdf", "xlsx"),
    PDF_TO_HTML("PDF 转 HTML", "pdf", "html"),
    PDF_TO_IMAGES("PDF 全部页转图片（ZIP）", "pdf", "zip"),
    PDF_COMPRESS("PDF 压缩优化", "pdf", "pdf"),

    // ===== PPT 相关 =====
    SLIDES_TO_PDF("演示文稿转 PDF", "pptx,ppt", "pdf"),
    SLIDES_TO_HTML("演示文稿转 HTML", "pptx,ppt", "html"),
    SLIDES_TO_IMAGES("演示文稿全部页转图片（ZIP）", "pptx,ppt", "zip"),

    // ===== Word 相关 =====
    WORDS_TO_PDF("Word 转 PDF", "docx,doc", "pdf"),
    WORDS_TO_HTML("Word 转 HTML", "docx,doc", "html"),
    WORDS_TO_MARKDOWN("Word 转 Markdown", "docx,doc", "md"),
    WORDS_TO_IMAGE("Word 首页转图片", "docx,doc", "png"),
    WORDS_TO_IMAGES("Word 全部页转图片（ZIP）", "docx,doc", "zip");

    /**
     * 转换类型展示名称
     */
    private final String displayName;

    /**
     * 支持的源文件扩展名（逗号分隔）
     */
    private final String sourceExtensions;

    /**
     * 目标文件扩展名
     */
    private final String targetExtension;

}
