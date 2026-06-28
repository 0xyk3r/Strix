package cn.projectan.strix.util.document;

import cn.projectan.strix.core.exception.StrixException;
import com.aspose.slides.*;
import lombok.extern.slf4j.Slf4j;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Aspose.Slides 工具类（PPT/PPTX 处理）
 * <p>
 * 支持格式转换、文本提取、内容编辑、占位符填充、合并拆分、水印等功能。
 * 所有方法均为静态方法，无需实例化。
 * </p>
 *
 * @author ProjectAn
 * @since 2026/6/28 12:46
 */
@Slf4j
public class AsposeSlidesUtil {

    static {
        init();
    }

    private static void init() {
        try {
            InputStream is = AsposeSlidesUtil.class.getResourceAsStream("/other/license.xml");
            if (is == null) {
                log.error("[Strix Document Slides] 未找到授权文件, 初始化失败.");
                return;
            }
            License license = new License();
            license.setLicense(is);
            log.info("[Strix Document Slides] 授权文件加载成功.");
        } catch (Exception e) {
            log.error("[Strix Document Slides] 授权文件加载失败.", e);
            throw new StrixException("Strix Document Slides 授权文件加载失败: " + e.getMessage(), e);
        }
    }

    // ==================== 格式转换 ====================

    /**
     * PPT/PPTX 转 PDF
     *
     * @param ppt PPT 输入流
     * @param out PDF 输出流
     */
    public static void toPdf(InputStream ppt, OutputStream out) throws Exception {
        Presentation pres = new Presentation(ppt);
        try {
            pres.save(out, SaveFormat.Pdf);
        } finally {
            pres.dispose();
        }
    }

    /**
     * 将每张幻灯片转为图片（PNG）
     *
     * @param ppt    PPT 输入流
     * @param width  输出图片宽度（像素）
     * @param height 输出图片高度（像素）
     * @param out    ZIP 压缩包输出流（包含每页 PNG 图片，命名格式：slide_001.png）
     */
    public static void toImages(InputStream ppt, int width, int height, OutputStream out) throws Exception {
        toImages(ppt, width, height, out, null);
    }

    /**
     * 将每张幻灯片转为图片（PNG），支持逐页进度回调
     *
     * @param ppt              PPT 输入流
     * @param width            输出图片宽度（像素）
     * @param height           输出图片高度（像素）
     * @param out              ZIP 压缩包输出流
     * @param progressCallback 逐页进度回调，接收当前幻灯片序号（1-based），为 null 时忽略
     */
    public static void toImages(InputStream ppt, int width, int height, OutputStream out,
                                java.util.function.IntConsumer progressCallback) throws Exception {
        Presentation pres = new Presentation(ppt);
        try {
            ISlideCollection slides = pres.getSlides();
            int totalSlides = slides.size();
            try (ZipOutputStream zipOut = new ZipOutputStream(out)) {
                for (int i = 0; i < totalSlides; i++) {
                    ISlide slide = slides.get_Item(i);
                    BufferedImage bufferedImage = slide.getThumbnail(new Dimension(width, height));
                    ByteArrayOutputStream imgOut = new ByteArrayOutputStream();
                    ImageIO.write(bufferedImage, "PNG", imgOut);
                    zipOut.putNextEntry(new ZipEntry(String.format("slide_%03d.png", i + 1)));
                    zipOut.write(imgOut.toByteArray());
                    zipOut.closeEntry();
                    if (progressCallback != null) progressCallback.accept(i + 1);
                }
            }
        } finally {
            pres.dispose();
        }
    }

    /**
     * PPTX 转 HTML
     *
     * @param ppt PPT 输入流
     * @param out HTML 输出流
     */
    public static void toHtml(InputStream ppt, OutputStream out) throws Exception {
        Presentation pres = new Presentation(ppt);
        try {
            pres.save(out, SaveFormat.Html);
        } finally {
            pres.dispose();
        }
    }

    // ==================== 读取 ====================

    /**
     * 提取每张幻灯片的所有文本（包含形状和文本框内的文字）
     *
     * @param ppt PPT 输入流
     * @return 每页文本字符串列表，顺序与幻灯片一致
     */
    public static List<String> extractText(InputStream ppt) throws Exception {
        List<String> texts = new ArrayList<>();
        Presentation pres = new Presentation(ppt);
        try {
            for (ISlide slide : pres.getSlides()) {
                StringBuilder sb = new StringBuilder();
                for (IShape shape : slide.getShapes()) {
                    if (shape instanceof IAutoShape autoShape) {
                        ITextFrame textFrame = autoShape.getTextFrame();
                        if (textFrame != null) {
                            for (IParagraph para : textFrame.getParagraphs()) {
                                for (IPortion portion : para.getPortions()) {
                                    sb.append(portion.getText());
                                }
                                sb.append("\n");
                            }
                        }
                    }
                }
                texts.add(sb.toString().trim());
            }
        } finally {
            pres.dispose();
        }
        return texts;
    }

    /**
     * 获取演示文稿的幻灯片数量
     *
     * @param ppt PPT 输入流
     * @return 幻灯片数量
     */
    public static int getSlideCount(InputStream ppt) throws Exception {
        Presentation pres = new Presentation(ppt);
        try {
            return pres.getSlides().size();
        } finally {
            pres.dispose();
        }
    }

    /**
     * 提取演示文稿中所有图片
     *
     * @param ppt PPT 输入流
     * @return 图片字节数组列表
     */
    public static List<byte[]> extractImages(InputStream ppt) throws Exception {
        List<byte[]> images = new ArrayList<>();
        Presentation pres = new Presentation(ppt);
        try {
            for (ISlide slide : pres.getSlides()) {
                for (IShape shape : slide.getShapes()) {
                    if (shape instanceof IPictureFrame picFrame) {
                        IPPImage pic = picFrame.getPictureFormat().getPicture().getImage();
                        images.add(pic.getBinaryData());
                    }
                }
            }
        } finally {
            pres.dispose();
        }
        return images;
    }

    // ==================== 创建 / 编辑 ====================

    /**
     * 向演示文稿末尾追加一张文本幻灯片
     *
     * @param ppt   PPT 输入流
     * @param title 幻灯片标题
     * @param body  幻灯片正文内容
     * @param out   输出流
     */
    public static void addTextSlide(InputStream ppt, String title, String body, OutputStream out) throws Exception {
        Presentation pres = new Presentation(ppt);
        try {
            ILayoutSlide layout = pres.getLayoutSlides().get_Item(0);
            ISlide newSlide = pres.getSlides().addEmptySlide(layout);

            IAutoShape titleShape = newSlide.getShapes().addAutoShape(
                    ShapeType.Rectangle, 50, 30,
                    (float) pres.getSlideSize().getSize().getWidth() - 100, 80);
            titleShape.getTextFrame().setText(title);
            titleShape.getTextFrame().getParagraphs().get_Item(0).getPortions().get_Item(0)
                    .getPortionFormat().setFontHeight(28);

            IAutoShape bodyShape = newSlide.getShapes().addAutoShape(
                    ShapeType.Rectangle, 50, 130,
                    (float) pres.getSlideSize().getSize().getWidth() - 100,
                    (float) pres.getSlideSize().getSize().getHeight() - 180);
            bodyShape.getTextFrame().setText(body);
            bodyShape.getTextFrame().getParagraphs().get_Item(0).getPortions().get_Item(0)
                    .getPortionFormat().setFontHeight(18);

            pres.save(out, SaveFormat.Pptx);
        } finally {
            pres.dispose();
        }
    }

    /**
     * 填充幻灯片中的占位符（占位符格式：{@code {{key}}}）
     * <p>
     * 遍历所有幻灯片的文本内容，将 {@code {{key}}} 替换为对应值。
     * </p>
     *
     * @param ppt  PPT 输入流
     * @param data 键值对数据，key 对应占位符名称
     * @param out  输出流
     */
    public static void fillPlaceholders(InputStream ppt, Map<String, String> data, OutputStream out) throws Exception {
        Presentation pres = new Presentation(ppt);
        try {
            for (ISlide slide : pres.getSlides()) {
                for (IShape shape : slide.getShapes()) {
                    if (shape instanceof IAutoShape autoShape) {
                        ITextFrame textFrame = autoShape.getTextFrame();
                        if (textFrame != null) {
                            for (IParagraph para : textFrame.getParagraphs()) {
                                for (IPortion portion : para.getPortions()) {
                                    String text = portion.getText();
                                    for (Map.Entry<String, String> entry : data.entrySet()) {
                                        text = text.replace("{{" + entry.getKey() + "}}", entry.getValue());
                                    }
                                    portion.setText(text);
                                }
                            }
                        }
                    }
                }
            }
            pres.save(out, SaveFormat.Pptx);
        } finally {
            pres.dispose();
        }
    }

    /**
     * 在指定幻灯片中插入图片
     *
     * @param ppt        PPT 输入流
     * @param slideIndex 幻灯片下标（0-based）
     * @param image      图片输入流
     * @param x          图片左上角 X 坐标（EMU 点）
     * @param y          图片左上角 Y 坐标（EMU 点）
     * @param width      图片宽度（EMU 点）
     * @param height     图片高度（EMU 点）
     * @param out        输出流
     */
    public static void addImageToSlide(InputStream ppt, int slideIndex, InputStream image,
                                       float x, float y, float width, float height,
                                       OutputStream out) throws Exception {
        Presentation pres = new Presentation(ppt);
        try {
            IPPImage ppImage = pres.getImages().addImage(image);
            ISlide slide = pres.getSlides().get_Item(slideIndex);
            slide.getShapes().addPictureFrame(ShapeType.Rectangle, x, y, width, height, ppImage);
            pres.save(out, SaveFormat.Pptx);
        } finally {
            pres.dispose();
        }
    }

    // ==================== 合并 / 拆分 ====================

    /**
     * 合并多个演示文稿（按顺序追加幻灯片）
     *
     * @param ppts 待合并的 PPT 输入流列表（第一个文件为基础）
     * @param out  合并后的输出流
     */
    public static void merge(List<InputStream> ppts, OutputStream out) throws Exception {
        if (ppts == null || ppts.isEmpty()) {
            return;
        }
        Presentation target = new Presentation(ppts.get(0));
        try {
            for (int i = 1; i < ppts.size(); i++) {
                Presentation src = new Presentation(ppts.get(i));
                try {
                    for (ISlide slide : src.getSlides()) {
                        target.getSlides().addClone(slide);
                    }
                } finally {
                    src.dispose();
                }
            }
            target.save(out, SaveFormat.Pptx);
        } finally {
            target.dispose();
        }
    }

    /**
     * 提取指定幻灯片到新演示文稿
     *
     * @param ppt          PPT 输入流
     * @param slideIndexes 需要提取的幻灯片下标数组（0-based）
     * @param out          输出流
     */
    public static void extractSlides(InputStream ppt, int[] slideIndexes, OutputStream out) throws Exception {
        Presentation src = new Presentation(ppt);
        Presentation target = new Presentation();
        try {
            // 移除目标演示文稿中默认的空白幻灯片
            while (target.getSlides().size() > 0) {
                target.getSlides().removeAt(0);
            }
            for (int index : slideIndexes) {
                target.getSlides().addClone(src.getSlides().get_Item(index));
            }
            target.save(out, SaveFormat.Pptx);
        } finally {
            src.dispose();
            target.dispose();
        }
    }

    // ==================== 水印 ====================

    /**
     * 向所有幻灯片添加文字水印（居中、灰色、36pt）
     *
     * @param ppt  PPT 输入流
     * @param text 水印文字
     * @param out  输出流
     */
    public static void addTextWatermark(InputStream ppt, String text, OutputStream out) throws Exception {
        Presentation pres = new Presentation(ppt);
        try {
            float slideWidth = (float) pres.getSlideSize().getSize().getWidth();
            float slideHeight = (float) pres.getSlideSize().getSize().getHeight();

            for (ISlide slide : pres.getSlides()) {
                IAutoShape watermark = slide.getShapes().addAutoShape(
                        ShapeType.Rectangle, 0, 0, slideWidth, slideHeight);
                watermark.getFillFormat().setFillType(FillType.NoFill);
                watermark.getLineFormat().getFillFormat().setFillType(FillType.NoFill);

                ITextFrame tf = watermark.getTextFrame();
                tf.setText(text);
                IParagraph para = tf.getParagraphs().get_Item(0);
                para.getParagraphFormat().setAlignment(TextAlignment.Center);

                IPortion portion = para.getPortions().get_Item(0);
                portion.getPortionFormat().setFontHeight(36);
                portion.getPortionFormat().getFillFormat().setFillType(FillType.Solid);
                portion.getPortionFormat().getFillFormat().getSolidFillColor()
                        .setColor(new java.awt.Color(200, 200, 200, 100));

                watermark.getAutoShapeLock().setSelectLocked(true);
            }
            pres.save(out, SaveFormat.Pptx);
        } finally {
            pres.dispose();
        }
    }

}
