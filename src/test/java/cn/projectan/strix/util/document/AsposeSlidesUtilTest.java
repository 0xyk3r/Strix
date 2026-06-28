package cn.projectan.strix.util.document;

import com.aspose.slides.*;
import org.junit.jupiter.api.*;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * AsposeSlidesUtil 单元测试
 *
 * @author ProjectAn
 * @since 2026/6/28
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class AsposeSlidesUtilTest {

    /**
     * 供多个测试复用的简单 PPTX 字节数组
     */
    private static byte[] samplePptx;

    @BeforeAll
    static void setUp() throws Exception {
        // 用 Aspose.Slides API 创建一个简单演示文稿
        Presentation pres = new Presentation();
        try {
            // 第一张幻灯片（默认已存在），添加文本
            ISlide slide1 = pres.getSlides().get_Item(0);
            IAutoShape shape = slide1.getShapes().addAutoShape(
                    ShapeType.Rectangle, 100, 100, 500, 150);
            shape.getTextFrame().setText("Strix 文档转换测试 - 第一页");

            // 添加第二张幻灯片
            ILayoutSlide layout = pres.getLayoutSlides().get_Item(0);
            ISlide slide2 = pres.getSlides().addEmptySlide(layout);
            IAutoShape shape2 = slide2.getShapes().addAutoShape(
                    ShapeType.Rectangle, 100, 100, 500, 150);
            shape2.getTextFrame().setText("第二页：演示内容");

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            pres.save(baos, SaveFormat.Pptx);
            samplePptx = baos.toByteArray();
        } finally {
            pres.dispose();
        }
    }

    // ==================== 读取 ====================

    @Test
    @Order(1)
    @DisplayName("getSlideCount - 获取幻灯片数量")
    void getSlideCount() throws Exception {
        int count = AsposeSlidesUtil.getSlideCount(new ByteArrayInputStream(samplePptx));
        assertEquals(2, count, "应有 2 张幻灯片");
    }

    @Test
    @Order(2)
    @DisplayName("extractText - 提取幻灯片文本")
    void extractText() throws Exception {
        List<String> texts = AsposeSlidesUtil.extractText(new ByteArrayInputStream(samplePptx));
        assertNotNull(texts);
        assertEquals(2, texts.size());
        assertTrue(texts.get(0).contains("Strix") || texts.get(0).contains("第一页"),
                "第一页应包含对应文本");
        assertTrue(texts.get(1).contains("第二页"), "第二页应包含对应文本");
    }

    // ==================== 格式转换 ====================

    @Test
    @Order(3)
    @DisplayName("toPdf - PPTX 转 PDF")
    void toPdf() throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        AsposeSlidesUtil.toPdf(new ByteArrayInputStream(samplePptx), out);
        assertTrue(out.size() > 0, "PDF 不应为空");
        // PDF 文件头
        byte[] pdfBytes = out.toByteArray();
        assertEquals('%', (char) pdfBytes[0]);
        assertEquals('P', (char) pdfBytes[1]);
        assertEquals('D', (char) pdfBytes[2]);
        assertEquals('F', (char) pdfBytes[3]);
    }

    @Test
    @Order(4)
    @DisplayName("toImages - PPTX 每页转图片（ZIP）")
    void toImages() throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        AsposeSlidesUtil.toImages(new ByteArrayInputStream(samplePptx), 800, 600, out);
        assertTrue(out.size() > 0, "ZIP 文件不应为空");
        // ZIP 文件头: PK
        byte[] zipBytes = out.toByteArray();
        assertEquals('P', (char) zipBytes[0]);
        assertEquals('K', (char) zipBytes[1]);
    }

    @Test
    @Order(5)
    @DisplayName("toHtml - PPTX 转 HTML")
    void toHtml() throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        AsposeSlidesUtil.toHtml(new ByteArrayInputStream(samplePptx), out);
        assertTrue(out.size() > 0);
    }

    // ==================== 编辑 ====================

    @Test
    @Order(6)
    @DisplayName("addTextSlide - 追加文本幻灯片")
    void addTextSlide() throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        AsposeSlidesUtil.addTextSlide(new ByteArrayInputStream(samplePptx),
                "新增标题", "新增正文内容", out);
        assertTrue(out.size() > 0);
        // 验证幻灯片数量增加
        int count = AsposeSlidesUtil.getSlideCount(new ByteArrayInputStream(out.toByteArray()));
        assertEquals(3, count, "应变为 3 张幻灯片");
    }

    @Test
    @Order(7)
    @DisplayName("fillPlaceholders - 占位符填充")
    void fillPlaceholders() throws Exception {
        // 创建带占位符的演示文稿
        Presentation pres = new Presentation();
        try {
            ISlide slide = pres.getSlides().get_Item(0);
            IAutoShape shape = slide.getShapes().addAutoShape(
                    ShapeType.Rectangle, 100, 100, 600, 150);
            shape.getTextFrame().setText("公司：{{company}}，年份：{{year}}");

            ByteArrayOutputStream templateBaos = new ByteArrayOutputStream();
            pres.save(templateBaos, SaveFormat.Pptx);

            Map<String, String> data = new LinkedHashMap<>();
            data.put("company", "Strix 科技");
            data.put("year", "2026");

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            AsposeSlidesUtil.fillPlaceholders(
                    new ByteArrayInputStream(templateBaos.toByteArray()), data, out);
            assertTrue(out.size() > 0);

            // 验证替换结果
            List<String> texts = AsposeSlidesUtil.extractText(new ByteArrayInputStream(out.toByteArray()));
            assertTrue(texts.get(0).contains("Strix 科技"), "应包含替换后的公司名");
            assertTrue(texts.get(0).contains("2026"), "应包含替换后的年份");
        } finally {
            pres.dispose();
        }
    }

    // ==================== 合并 / 拆分 ====================

    @Test
    @Order(8)
    @DisplayName("merge - 合并演示文稿")
    void merge() throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        AsposeSlidesUtil.merge(
                Arrays.asList(
                        new ByteArrayInputStream(samplePptx),
                        new ByteArrayInputStream(samplePptx)
                ), out);
        assertTrue(out.size() > 0);
        int mergedCount = AsposeSlidesUtil.getSlideCount(new ByteArrayInputStream(out.toByteArray()));
        assertEquals(4, mergedCount, "合并后应有 4 张幻灯片");
    }

    @Test
    @Order(9)
    @DisplayName("extractSlides - 提取指定幻灯片")
    void extractSlides() throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        AsposeSlidesUtil.extractSlides(new ByteArrayInputStream(samplePptx), new int[]{0}, out);
        assertTrue(out.size() > 0);
        assertEquals(1, AsposeSlidesUtil.getSlideCount(new ByteArrayInputStream(out.toByteArray())),
                "提取后应只有 1 张幻灯片");
    }

    // ==================== 水印 ====================

    @Test
    @Order(10)
    @DisplayName("addTextWatermark - 添加文字水印")
    void addTextWatermark() throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        AsposeSlidesUtil.addTextWatermark(new ByteArrayInputStream(samplePptx), "仅供测试", out);
        assertTrue(out.size() > 0);
    }

}
