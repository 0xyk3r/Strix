package cn.projectan.strix.util.document;

import com.aspose.words.*;
import com.aspose.words.Shape;
import com.aspose.words.ref.Ref;
import lombok.extern.slf4j.Slf4j;

import java.awt.*;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Aspose.Words 工具类（Word 文档处理）
 * <p>
 * 支持格式转换、文本提取、邮件合并、书签填充、查找替换、水印、页眉页脚、文档合并、保护等功能。
 * 所有方法均为静态方法，无需实例化。
 * </p>
 *
 * @author ProjectAn
 * @since 2026/6/28 12:45
 */
@Slf4j
public class AsposeWordsUtil {

    // ==================== 格式转换 ====================

    /**
     * DOC/DOCX 转 PDF
     *
     * @param doc DOC/DOCX 输入流
     * @param out PDF 输出流
     */
    public static void toPdf(InputStream doc, OutputStream out) throws Exception {
        Document document = new Document(doc);
        document.save(out, SaveFormat.PDF);
    }

    /**
     * DOCX 转 HTML（自包含，图片以 Base64 内嵌）
     *
     * @param doc DOC/DOCX 输入流
     * @param out HTML 输出流
     */
    public static void toHtml(InputStream doc, OutputStream out) throws Exception {
        Document document = new Document(doc);
        HtmlSaveOptions saveOptions = new HtmlSaveOptions();
        saveOptions.setExportImagesAsBase64(true);
        saveOptions.setCssStyleSheetType(CssStyleSheetType.EMBEDDED);
        document.save(out, saveOptions);
    }

    /**
     * DOCX 转 Markdown
     * <p>
     * 使用 {@code MarkdownSaveOptions} 并将图片以 Base64 内嵌，
     * 避免保存至 OutputStream 时因无法写入图片文件路径而抛出 {@code IllegalStateException}。
     * </p>
     *
     * @param doc DOC/DOCX 输入流
     * @param out Markdown 输出流
     */
    public static void toMarkdown(InputStream doc, OutputStream out) throws Exception {
        Document document = new Document(doc);
        MarkdownSaveOptions options = new MarkdownSaveOptions();
        // 将图片以 Base64 内嵌到 Markdown，避免 OutputStream 模式下找不到文件路径
        options.setExportImagesAsBase64(true);
        document.save(out, options);
    }

    /**
     * 将指定页面转为图片（PNG）
     *
     * @param doc       DOC/DOCX 输入流
     * @param pageIndex 页面下标（0-based）
     * @param out       图片输出流
     */
    public static void toImage(InputStream doc, int pageIndex, OutputStream out) throws Exception {
        Document document = new Document(doc);
        ImageSaveOptions saveOptions = new ImageSaveOptions(SaveFormat.PNG);
        saveOptions.setPageSet(new PageSet(pageIndex));
        document.save(out, saveOptions);
    }

    /**
     * 全部页面转图片并打包为 ZIP
     * <p>
     * 每页生成一个 PNG 文件，命名格式：page_001.png。
     * </p>
     *
     * @param doc DOC/DOCX 输入流
     * @param out ZIP 压缩包输出流
     */
    public static void toImages(InputStream doc, OutputStream out) throws Exception {
        toImages(doc, out, null);
    }

    /**
     * 全部页面转图片并打包为 ZIP，支持逐页进度回调
     *
     * @param doc              DOC/DOCX 输入流
     * @param out              ZIP 压缩包输出流
     * @param progressCallback 逐页进度回调，接收当前页序号（1-based），为 null 时忽略
     */
    public static void toImages(InputStream doc, OutputStream out,
                                java.util.function.IntConsumer progressCallback) throws Exception {
        Document document = new Document(doc);
        int pageCount = document.getPageCount();
        try (ZipOutputStream zipOut = new ZipOutputStream(out)) {
            for (int i = 0; i < pageCount; i++) {
                ImageSaveOptions saveOptions = new ImageSaveOptions(SaveFormat.PNG);
                saveOptions.setPageSet(new PageSet(i));
                ByteArrayOutputStream pageOut = new ByteArrayOutputStream();
                document.save(pageOut, saveOptions);
                zipOut.putNextEntry(new ZipEntry(String.format("page_%03d.png", i + 1)));
                zipOut.write(pageOut.toByteArray());
                zipOut.closeEntry();
                if (progressCallback != null) progressCallback.accept(i + 1);
            }
        }
    }

    /**
     * HTML 字符串转 DOCX
     *
     * @param html HTML 字符串
     * @param out  DOCX 输出流
     */
    public static void htmlToDocx(String html, OutputStream out) throws Exception {
        HtmlLoadOptions loadOptions = new HtmlLoadOptions();
        Document document = new Document(
                new ByteArrayInputStream(html.getBytes(StandardCharsets.UTF_8)), loadOptions);
        document.save(out, SaveFormat.DOCX);
    }

    // ==================== 读取 ====================

    /**
     * 提取文档全文文本
     *
     * @param doc DOC/DOCX 输入流
     * @return 全文纯文本字符串
     */
    public static String extractText(InputStream doc) throws Exception {
        Document document = new Document(doc);
        return document.toString(SaveFormat.TEXT);
    }

    /**
     * 获取文档总页数
     *
     * @param doc DOC/DOCX 输入流
     * @return 总页数
     */
    public static int getPageCount(InputStream doc) throws Exception {
        Document document = new Document(doc);
        return document.getPageCount();
    }

    /**
     * 提取文档中所有内嵌图片
     *
     * @param doc DOC/DOCX 输入流
     * @return 图片字节数组列表
     */
    public static List<byte[]> extractImages(InputStream doc) throws Exception {
        Document document = new Document(doc);
        List<byte[]> images = new ArrayList<>();
        NodeCollection shapes = document.getChildNodes(NodeType.SHAPE, true);
        for (Shape shape : (Iterable<Shape>) shapes) {
            if (shape.hasImage()) {
                images.add(shape.getImageData().getImageBytes());
            }
        }
        return images;
    }

    // ==================== 模板 / 邮件合并 ====================

    /**
     * 简单邮件合并（字段名 → 字段值，一对一映射）
     * <p>
     * 模板中的合并字段格式为 Word 标准合并域：{@code «FieldName»}（通过 Word 插入域）。
     * </p>
     *
     * @param template 模板 DOCX 输入流
     * @param data     键值对数据，key 对应合并域名称
     * @param out      输出流
     */
    public static void mailMerge(InputStream template, Map<String, Object> data, OutputStream out) throws Exception {
        Document document = new Document(template);
        String[] fieldNames = data.keySet().toArray(new String[0]);
        Object[] fieldValues = data.values().toArray(new Object[0]);
        document.getMailMerge().execute(fieldNames, fieldValues);
        document.save(out, SaveFormat.DOCX);
    }

    /**
     * 带区域的邮件合并（用于表格/列表数据批量填充）
     * <p>
     * 模板中需要有标准 Word 区域合并域：{@code «TableStart:regionName»} 和 {@code «TableEnd:regionName»}。
     * </p>
     *
     * @param template   模板 DOCX 输入流
     * @param regionName 合并区域名称，与模板中的区域标签一致
     * @param rows       数据行列表，每行为 Map&lt;字段名, 值&gt;
     * @param out        输出流
     */
    public static void mailMergeWithRegion(InputStream template, String regionName,
                                           List<Map<String, Object>> rows, OutputStream out) throws Exception {
        Document document = new Document(template);
        document.getMailMerge().executeWithRegions(new MapListMailMergeDataSource(regionName, rows));
        document.save(out, SaveFormat.DOCX);
    }

    /**
     * 基于书签填充文档内容
     * <p>
     * 根据书签名称定位书签，并将其内容替换为对应值。
     * </p>
     *
     * @param template  模板 DOCX 输入流
     * @param bookmarks 书签名称 → 填充内容 的映射
     * @param out       输出流
     */
    public static void fillBookmarks(InputStream template, Map<String, String> bookmarks, OutputStream out) throws Exception {
        Document document = new Document(template);
        DocumentBuilder builder = new DocumentBuilder(document);
        for (Map.Entry<String, String> entry : bookmarks.entrySet()) {
            Bookmark bookmark = document.getRange().getBookmarks().get(entry.getKey());
            if (bookmark != null) {
                builder.moveToBookmark(entry.getKey());
                builder.write(entry.getValue());
            } else {
                log.warn("[Strix Document Words] 书签未找到: {}", entry.getKey());
            }
        }
        document.save(out, SaveFormat.DOCX);
    }

    // ==================== 内容操作 ====================

    /**
     * 全文查找并替换文本
     *
     * @param doc     DOC/DOCX 输入流
     * @param search  搜索文本
     * @param replace 替换文本
     * @param out     输出流
     */
    public static void findAndReplace(InputStream doc, String search, String replace, OutputStream out) throws Exception {
        Document document = new Document(doc);
        document.getRange().replace(search, replace, new FindReplaceOptions());
        document.save(out, SaveFormat.DOCX);
    }

    /**
     * 添加文字水印（45度斜体，灰色半透明）
     *
     * @param doc  DOC/DOCX 输入流
     * @param text 水印文字
     * @param out  输出流
     */
    public static void addWatermark(InputStream doc, String text, OutputStream out) throws Exception {
        Document document = new Document(doc);
        TextWatermarkOptions options = new TextWatermarkOptions();
        options.setFontSize(72);
        options.setColor(Color.LIGHT_GRAY);
        options.setLayout(WatermarkLayout.DIAGONAL);
        options.isSemitrasparent(true);
        document.getWatermark().setText(text, options);
        document.save(out, SaveFormat.DOCX);
    }

    /**
     * 添加图片水印
     *
     * @param doc   DOC/DOCX 输入流
     * @param image 水印图片输入流
     * @param out   输出流
     */
    public static void addImageWatermark(InputStream doc, InputStream image, OutputStream out) throws Exception {
        Document document = new Document(doc);
        java.awt.image.BufferedImage bufferedImage = javax.imageio.ImageIO.read(image);
        ImageWatermarkOptions options = new ImageWatermarkOptions();
        options.setScale(0.5);
        options.isWashout(true);
        document.getWatermark().setImage(bufferedImage, options);
        document.save(out, SaveFormat.DOCX);
    }

    /**
     * 设置文档主页眉
     *
     * @param doc        DOC/DOCX 输入流
     * @param headerText 页眉文字
     * @param out        输出流
     */
    public static void insertHeader(InputStream doc, String headerText, OutputStream out) throws Exception {
        Document document = new Document(doc);
        DocumentBuilder builder = new DocumentBuilder(document);
        builder.moveToHeaderFooter(HeaderFooterType.HEADER_PRIMARY);
        builder.getParagraphFormat().setAlignment(ParagraphAlignment.CENTER);
        builder.write(headerText);
        document.save(out, SaveFormat.DOCX);
    }

    /**
     * 设置文档主页脚（可选添加页码）
     *
     * @param doc           DOC/DOCX 输入流
     * @param footerText    页脚文字（为空时仅显示页码）
     * @param addPageNumber 是否添加页码（格式：第 X 页 / 共 Y 页）
     * @param out           输出流
     */
    public static void insertFooter(InputStream doc, String footerText, boolean addPageNumber, OutputStream out) throws Exception {
        Document document = new Document(doc);
        DocumentBuilder builder = new DocumentBuilder(document);
        builder.moveToHeaderFooter(HeaderFooterType.FOOTER_PRIMARY);
        builder.getParagraphFormat().setAlignment(ParagraphAlignment.CENTER);
        if (footerText != null && !footerText.isBlank()) {
            builder.write(footerText);
        }
        if (addPageNumber) {
            if (footerText != null && !footerText.isBlank()) {
                builder.write("  ");
            }
            builder.write("第 ");
            builder.insertField("PAGE");
            builder.write(" 页 / 共 ");
            builder.insertField("NUMPAGES");
            builder.write(" 页");
        }
        document.save(out, SaveFormat.DOCX);
    }

    // ==================== 合并 / 拆分 ====================

    /**
     * 合并多个 Word 文档（保留各自格式，按列表顺序合并）
     *
     * @param docs 待合并的文档输入流列表（第一个文件为基础）
     * @param out  合并后的 DOCX 输出流
     */
    public static void merge(List<InputStream> docs, OutputStream out) throws Exception {
        if (docs == null || docs.isEmpty()) {
            return;
        }
        Document dstDoc = new Document(docs.get(0));
        for (int i = 1; i < docs.size(); i++) {
            Document srcDoc = new Document(docs.get(i));
            dstDoc.appendDocument(srcDoc, ImportFormatMode.KEEP_SOURCE_FORMATTING);
        }
        dstDoc.save(out, SaveFormat.DOCX);
    }

    /**
     * 按节（Section）拆分文档
     * <p>
     * 将文档的每一节拆分为独立文档，适用于含有章节分节符的文档。
     * </p>
     *
     * @param doc DOC/DOCX 输入流
     * @return 每节对应的 DOCX 字节数组列表
     */
    public static List<byte[]> splitBySection(InputStream doc) throws Exception {
        Document document = new Document(doc);
        List<byte[]> result = new ArrayList<>();
        for (Section section : document.getSections()) {
            Document sectionDoc = new Document();
            sectionDoc.getSections().clear();
            Section importedSection = (Section) sectionDoc.importNode(section, true);
            sectionDoc.getSections().add(importedSection);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            sectionDoc.save(baos, SaveFormat.DOCX);
            result.add(baos.toByteArray());
        }
        return result;
    }

    // ==================== 保护 ====================

    /**
     * 对文档添加只读保护（需要密码才能编辑）
     *
     * @param doc      DOC/DOCX 输入流
     * @param password 保护密码
     * @param out      输出流
     */
    public static void protect(InputStream doc, String password, OutputStream out) throws Exception {
        Document document = new Document(doc);
        document.protect(ProtectionType.READ_ONLY, password);
        document.save(out, SaveFormat.DOCX);
    }

    /**
     * 移除文档保护
     *
     * @param doc      已保护的 DOC/DOCX 输入流
     * @param password 保护密码
     * @param out      输出流
     */
    public static void removeProtection(InputStream doc, String password, OutputStream out) throws Exception {
        Document document = new Document(doc);
        document.unprotect(password);
        document.save(out, SaveFormat.DOCX);
    }

    // ==================== 其他 ====================

    /**
     * 接受文档中的所有修订（追踪更改）
     *
     * @param doc DOC/DOCX 输入流
     * @param out 输出流
     */
    public static void acceptAllRevisions(InputStream doc, OutputStream out) throws Exception {
        Document document = new Document(doc);
        document.acceptAllRevisions();
        document.save(out, SaveFormat.DOCX);
    }

    /**
     * 更新文档中的所有域（包括目录 TOC）
     * <p>
     * 文档中若含有 TOC 域，调用此方法可自动生成/更新目录。
     * </p>
     *
     * @param doc DOC/DOCX 输入流
     * @param out 输出流
     */
    public static void updateFields(InputStream doc, OutputStream out) throws Exception {
        Document document = new Document(doc);
        document.updateFields();
        document.save(out, SaveFormat.DOCX);
    }

    // ==================== 内部类 ====================

    /**
     * 用于 mailMergeWithRegion 的 IMailMergeDataSource 实现
     */
    private static class MapListMailMergeDataSource implements IMailMergeDataSource {

        private final String tableName;
        private final List<Map<String, Object>> rows;
        private int currentIndex = -1;

        public MapListMailMergeDataSource(String tableName, List<Map<String, Object>> rows) {
            this.tableName = tableName;
            this.rows = rows;
        }

        @Override
        public String getTableName() {
            return tableName;
        }

        @Override
        public boolean moveNext() {
            currentIndex++;
            return currentIndex < rows.size();
        }

        @Override
        public boolean getValue(String fieldName, Ref<Object> fieldValue) throws Exception {
            Map<String, Object> currentRow = rows.get(currentIndex);
            if (currentRow.containsKey(fieldName)) {
                fieldValue.set(currentRow.get(fieldName));
                return true;
            }
            fieldValue.set(null);
            return false;
        }

        @Override
        public IMailMergeDataSource getChildDataSource(String tableName) {
            return null;
        }
    }

}
