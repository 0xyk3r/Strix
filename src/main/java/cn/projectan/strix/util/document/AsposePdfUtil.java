package cn.projectan.strix.util.document;

import cn.projectan.strix.core.exception.StrixException;
import com.aspose.pdf.*;
import com.aspose.pdf.devices.PngDevice;
import com.aspose.pdf.devices.Resolution;
import com.aspose.pdf.facades.PdfFileEditor;
import com.aspose.pdf.optimization.OptimizationOptions;
import lombok.extern.slf4j.Slf4j;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Aspose.PDF 工具类（PDF 处理）
 * <p>
 * 支持格式转换、创建、合并拆分、内容操作、页面操作、安全加密、压缩优化等功能。
 * 所有方法均为静态方法，无需实例化。
 * </p>
 *
 * @author ProjectAn
 * @since 2026/6/28 12:47
 */
@Slf4j
public class AsposePdfUtil {

    static {
        init();
    }

    private static void init() {
        try {
            InputStream is = AsposePdfUtil.class.getResourceAsStream("/other/license.xml");
            if (is == null) {
                log.error("[Strix Document PDF] 未找到授权文件, 初始化失败.");
                return;
            }
            License license = new License();
            license.setLicense(is);
            log.info("[Strix Document PDF] 授权文件加载成功.");
        } catch (Exception e) {
            log.error("[Strix Document PDF] 授权文件加载失败.", e);
            throw new StrixException("Strix Document PDF 授权文件加载失败: " + e.getMessage(), e);
        }
    }

    // ==================== 格式转换 ====================

    /**
     * PDF 转 Word（DOCX）
     *
     * @param pdf PDF 输入流
     * @param out DOCX 输出流
     */
    public static void toWord(InputStream pdf, OutputStream out) throws Exception {
        Document document = new Document(pdf);
        DocSaveOptions saveOptions = new DocSaveOptions();
        saveOptions.setFormat(DocSaveOptions.DocFormat.DocX);
        document.save(out, saveOptions);
    }

    /**
     * PDF 转 Excel（XLSX）
     *
     * @param pdf PDF 输入流
     * @param out XLSX 输出流
     */
    public static void toExcel(InputStream pdf, OutputStream out) throws Exception {
        Document document = new Document(pdf);
        ExcelSaveOptions saveOptions = new ExcelSaveOptions();
        saveOptions.setFormat(ExcelSaveOptions.ExcelFormat.XLSX);
        document.save(out, saveOptions);
    }

    /**
     * PDF 转 HTML（图片以 PNG 内嵌，输出单文件）
     *
     * @param pdf PDF 输入流
     * @param out HTML 输出流
     */
    public static void toHtml(InputStream pdf, OutputStream out) throws Exception {
        Document document = new Document(pdf);
        HtmlSaveOptions saveOptions = new HtmlSaveOptions();
        saveOptions.setPartsEmbeddingMode(HtmlSaveOptions.PartsEmbeddingModes.EmbedAllIntoHtml);
        // 使用 EmbedAllIntoHtml 模式时，栅格图像必须以 PNG 背景方式内嵌
        saveOptions.setRasterImagesSavingMode(HtmlSaveOptions.RasterImagesSavingModes.AsEmbeddedPartsOfPngPageBackground);
        document.save(out, saveOptions);
    }

    /**
     * PDF 每页转图片（PNG）
     *
     * @param pdf PDF 输入流
     * @param dpi 图片分辨率（推荐 150~300）
     * @param out ZIP 压缩包输出流（包含每页 PNG 图片，命名格式：page_001.png）
     */
    public static void toImages(InputStream pdf, int dpi, OutputStream out) throws Exception {
        toImages(pdf, dpi, out, null);
    }

    /**
     * PDF 每页转图片（PNG），支持逐页进度回调
     *
     * @param pdf              PDF 输入流
     * @param dpi              图片分辨率（推荐 150~300）
     * @param out              ZIP 压缩包输出流
     * @param progressCallback 逐页进度回调，接收当前页码（1-based），为 null 时忽略
     */
    public static void toImages(InputStream pdf, int dpi, OutputStream out,
                                java.util.function.IntConsumer progressCallback) throws Exception {
        Document document = new Document(pdf);
        int totalPages = document.getPages().size();
        Resolution resolution = new Resolution(dpi);
        PngDevice pngDevice = new PngDevice(resolution);
        try (ZipOutputStream zipOut = new ZipOutputStream(out)) {
            for (int i = 1; i <= totalPages; i++) {
                ByteArrayOutputStream pageOut = new ByteArrayOutputStream();
                pngDevice.process(document.getPages().get_Item(i), pageOut);
                zipOut.putNextEntry(new ZipEntry(String.format("page_%03d.png", i)));
                zipOut.write(pageOut.toByteArray());
                zipOut.closeEntry();
                if (progressCallback != null) progressCallback.accept(i);
            }
        }
    }

    // ==================== 创建 ====================

    /**
     * 从文本段落列表创建 PDF
     * <p>
     * 每个段落作为一个独立的文本块，由 PDF 引擎自动处理分页。
     * </p>
     *
     * @param paragraphs 文本段落列表
     * @param out        PDF 输出流
     */
    public static void createPdf(List<String> paragraphs, OutputStream out) throws Exception {
        Document document = new Document();
        Page page = document.getPages().add();
        for (String text : paragraphs) {
            TextFragment fragment = new TextFragment(text);
            fragment.getTextState().setFontSize(12);
            page.getParagraphs().add(fragment);
        }
        document.save(out);
    }

    /**
     * 将多张图片合并为一个 PDF（每张图片占一页）
     *
     * @param images 图片输入流列表
     * @param out    PDF 输出流
     */
    public static void imagesToPdf(List<InputStream> images, OutputStream out) throws Exception {
        Document document = new Document();
        for (InputStream imageStream : images) {
            Page page = document.getPages().add();
            page.setPageSize(PageSize.getA4().getWidth(), PageSize.getA4().getHeight());
            Image image = new Image();
            image.setImageStream(imageStream);
            image.setFixWidth(page.getPageInfo().getWidth()
                    - page.getPageInfo().getMargin().getLeft()
                    - page.getPageInfo().getMargin().getRight());
            page.getParagraphs().add(image);
        }
        document.save(out);
    }

    // ==================== 合并 / 拆分 ====================

    /**
     * 合并多个 PDF 为一个
     *
     * @param pdfs 待合并的 PDF 输入流列表
     * @param out  合并后的 PDF 输出流
     */
    public static void merge(List<InputStream> pdfs, OutputStream out) throws Exception {
        PdfFileEditor editor = new PdfFileEditor();
        InputStream[] streams = pdfs.toArray(new InputStream[0]);
        editor.concatenate(streams, out);
    }

    /**
     * 按页范围拆分 PDF
     *
     * @param pdf      PDF 输入流
     * @param pageFrom 起始页（1-based，含）
     * @param pageTo   结束页（1-based，含）
     * @param out      输出流
     */
    public static void split(InputStream pdf, int pageFrom, int pageTo, OutputStream out) throws Exception {
        PdfFileEditor editor = new PdfFileEditor();
        int[] pages = new int[pageTo - pageFrom + 1];
        for (int i = 0; i < pages.length; i++) {
            pages[i] = pageFrom + i;
        }
        editor.extract(pdf, pages, out);
    }

    /**
     * 提取指定页码的页面到新 PDF
     *
     * @param pdf         PDF 输入流
     * @param pageNumbers 需要提取的页码数组（1-based）
     * @param out         输出流
     */
    public static void extractPages(InputStream pdf, int[] pageNumbers, OutputStream out) throws Exception {
        PdfFileEditor editor = new PdfFileEditor();
        editor.extract(pdf, pageNumbers, out);
    }

    // ==================== 内容操作 ====================

    /**
     * 提取 PDF 全文文本
     *
     * @param pdf PDF 输入流
     * @return 全文文本字符串
     */
    public static String extractText(InputStream pdf) throws Exception {
        Document document = new Document(pdf);
        TextAbsorber absorber = new TextAbsorber();
        document.getPages().accept(absorber);
        return absorber.getText();
    }

    /**
     * 提取 PDF 中的所有图片
     *
     * @param pdf PDF 输入流
     * @return 图片字节数组列表
     */
    public static List<byte[]> extractImages(InputStream pdf) throws Exception {
        Document document = new Document(pdf);
        List<byte[]> images = new ArrayList<>();
        for (int i = 1; i <= document.getPages().size(); i++) {
            XImageCollection xImages = document.getPages().get_Item(i).getResources().getImages();
            for (int j = 1; j <= xImages.size(); j++) {
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                xImages.get_Item(j).save(baos);
                images.add(baos.toByteArray());
            }
        }
        return images;
    }

    /**
     * 添加文字水印（45度斜体，居中，半透明灰色）
     *
     * @param pdf  PDF 输入流
     * @param text 水印文字
     * @param out  输出流
     */
    public static void addWatermark(InputStream pdf, String text, OutputStream out) throws Exception {
        Document document = new Document(pdf);
        TextStamp stamp = new TextStamp(text);
        stamp.setRotateAngle(45);
        stamp.setOpacity(0.3);
        stamp.setHorizontalAlignment(HorizontalAlignment.Center);
        stamp.setVerticalAlignment(VerticalAlignment.Center);
        stamp.getTextState().setFontSize(72);
        stamp.getTextState().setForegroundColor(Color.getGray());
        for (int i = 1; i <= document.getPages().size(); i++) {
            document.getPages().get_Item(i).addStamp(stamp);
        }
        document.save(out);
    }

    /**
     * 添加图片水印（居中，半透明）
     *
     * @param pdf          PDF 输入流
     * @param watermarkImg 水印图片输入流
     * @param out          输出流
     */
    public static void addImageWatermark(InputStream pdf, InputStream watermarkImg, OutputStream out) throws Exception {
        Document document = new Document(pdf);
        ImageStamp stamp = new ImageStamp(watermarkImg);
        stamp.setOpacity(0.3);
        stamp.setHorizontalAlignment(HorizontalAlignment.Center);
        stamp.setVerticalAlignment(VerticalAlignment.Center);
        for (int i = 1; i <= document.getPages().size(); i++) {
            document.getPages().get_Item(i).addStamp(stamp);
        }
        document.save(out);
    }

    /**
     * 全文搜索并替换文本
     *
     * @param pdf     PDF 输入流
     * @param search  搜索文本
     * @param replace 替换文本
     * @param out     输出流
     */
    public static void searchAndReplace(InputStream pdf, String search, String replace, OutputStream out) throws Exception {
        Document document = new Document(pdf);
        TextFragmentAbsorber absorber = new TextFragmentAbsorber(search);
        document.getPages().accept(absorber);
        for (TextFragment fragment : absorber.getTextFragments()) {
            fragment.setText(replace);
        }
        document.save(out);
    }

    // ==================== 页面操作 ====================

    /**
     * 获取 PDF 总页数
     *
     * @param pdf PDF 输入流
     * @return 总页数
     */
    public static int getPageCount(InputStream pdf) throws Exception {
        Document document = new Document(pdf);
        return document.getPages().size();
    }

    /**
     * 旋转指定页面
     *
     * @param pdf        PDF 输入流
     * @param pageNumber 页码（1-based）
     * @param degree     旋转角度（支持 90、180、270）
     * @param out        输出流
     */
    public static void rotatePage(InputStream pdf, int pageNumber, int degree, OutputStream out) throws Exception {
        Document document = new Document(pdf);
        int rotation = switch (degree) {
            case 90 -> Rotation.on90;
            case 180 -> Rotation.on180;
            case 270 -> Rotation.on270;
            default -> Rotation.None;
        };
        document.getPages().get_Item(pageNumber).setRotate(rotation);
        document.save(out);
    }

    /**
     * 删除指定页面
     *
     * @param pdf        PDF 输入流
     * @param pageNumber 页码（1-based）
     * @param out        输出流
     */
    public static void deletePage(InputStream pdf, int pageNumber, OutputStream out) throws Exception {
        Document document = new Document(pdf);
        document.getPages().delete(pageNumber);
        document.save(out);
    }

    // ==================== 安全 ====================

    /**
     * 对 PDF 进行加密保护（AES 256 位）
     *
     * @param pdf       PDF 输入流
     * @param userPass  用户密码（打开文档用）
     * @param ownerPass 所有者密码（修改权限用）
     * @param out       输出流
     */
    public static void encrypt(InputStream pdf, String userPass, String ownerPass, OutputStream out) throws Exception {
        Document document = new Document(pdf);
        document.encrypt(userPass, ownerPass, 0, CryptoAlgorithm.AESx256);
        document.save(out);
    }

    /**
     * 解密 PDF（需要所有者密码）
     *
     * @param pdf      已加密的 PDF 输入流
     * @param password 所有者密码
     * @param out      解密后的输出流
     */
    public static void decrypt(InputStream pdf, String password, OutputStream out) throws Exception {
        Document document = new Document(pdf, password);
        document.decrypt();
        document.save(out);
    }

    // ==================== 优化 ====================

    /**
     * 压缩优化 PDF（缩小文件体积）
     * <p>
     * 启用图片压缩（75% 质量）、删除未使用对象、合并重复流。
     * </p>
     *
     * @param pdf PDF 输入流
     * @param out 压缩后的输出流
     */
    public static void compress(InputStream pdf, OutputStream out) throws Exception {
        Document document = new Document(pdf);
        OptimizationOptions options = new OptimizationOptions();
        options.setLinkDuplcateStreams(true);
        options.setRemoveUnusedObjects(true);
        options.setRemoveUnusedStreams(true);
        options.setCompressImages(true);
        options.setImageQuality(75);
        document.optimizeResources(options);
        document.save(out);
    }

}
