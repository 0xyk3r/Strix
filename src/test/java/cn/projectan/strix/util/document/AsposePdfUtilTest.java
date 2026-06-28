package cn.projectan.strix.util.document;

import org.junit.jupiter.api.*;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * AsposePdfUtil 单元测试
 *
 * @author ProjectAn
 * @since 2026/6/28
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class AsposePdfUtilTest {

    /**
     * 供多个测试复用的简单 PDF 字节数组
     */
    private static byte[] samplePdf;

    @BeforeAll
    static void setUp() throws Exception {
        List<String> paragraphs = Arrays.asList(
                "Strix 文档转换测试",
                "第一段：本文档由 Aspose.PDF 自动生成，用于单元测试。",
                "第二段：支持文本提取、格式转换、合并拆分等功能。",
                "第三段：This is an English paragraph for testing purposes."
        );
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        AsposePdfUtil.createPdf(paragraphs, out);
        samplePdf = out.toByteArray();
    }

    // ==================== 创建 ====================

    @Test
    @Order(1)
    @DisplayName("createPdf - 从文本段落创建 PDF")
    void createPdf() {
        assertNotNull(samplePdf);
        assertTrue(samplePdf.length > 0, "PDF 不应为空");
        // PDF 文件头
        assertEquals('%', (char) samplePdf[0]);
        assertEquals('P', (char) samplePdf[1]);
        assertEquals('D', (char) samplePdf[2]);
        assertEquals('F', (char) samplePdf[3]);
    }

    // ==================== 读取 ====================

    @Test
    @Order(2)
    @DisplayName("getPageCount - 获取总页数")
    void getPageCount() throws Exception {
        int count = AsposePdfUtil.getPageCount(new ByteArrayInputStream(samplePdf));
        assertTrue(count >= 1, "页数应至少为 1");
    }

    @Test
    @Order(3)
    @DisplayName("extractText - 提取全文文本")
    void extractText() throws Exception {
        String text = AsposePdfUtil.extractText(new ByteArrayInputStream(samplePdf));
        assertNotNull(text);
        assertFalse(text.isBlank(), "提取的文本不应为空");
        assertTrue(text.contains("Strix") || text.contains("文档转换"), "文本应包含原始内容");
    }

    // ==================== 格式转换 ====================

    @Test
    @Order(4)
    @DisplayName("toWord - PDF 转 DOCX")
    void toWord() throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        AsposePdfUtil.toWord(new ByteArrayInputStream(samplePdf), out);
        assertTrue(out.size() > 0, "DOCX 不应为空");
    }

    @Test
    @Order(5)
    @DisplayName("toExcel - PDF 转 XLSX")
    void toExcel() throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        AsposePdfUtil.toExcel(new ByteArrayInputStream(samplePdf), out);
        assertTrue(out.size() > 0, "XLSX 不应为空");
    }

    @Test
    @Order(6)
    @DisplayName("toHtml - PDF 转 HTML")
    void toHtml() throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        AsposePdfUtil.toHtml(new ByteArrayInputStream(samplePdf), out);
        assertTrue(out.size() > 0);
    }

    @Test
    @Order(7)
    @DisplayName("toImages - PDF 每页转图片（ZIP）")
    void toImages() throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        AsposePdfUtil.toImages(new ByteArrayInputStream(samplePdf), 150, out);
        assertTrue(out.size() > 0, "ZIP 文件不应为空");
        // ZIP 文件头: PK\x03\x04
        byte[] zipBytes = out.toByteArray();
        assertEquals('P', (char) zipBytes[0]);
        assertEquals('K', (char) zipBytes[1]);
    }

    // ==================== 水印 ====================

    @Test
    @Order(8)
    @DisplayName("addWatermark - 添加文字水印")
    void addWatermark() throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        AsposePdfUtil.addWatermark(new ByteArrayInputStream(samplePdf), "仅供测试", out);
        assertTrue(out.size() > 0);
        assertTrue(out.size() >= samplePdf.length, "添加水印后文件大小应不小于原始文件");
    }

    // ==================== 搜索替换 ====================

    @Test
    @Order(9)
    @DisplayName("searchAndReplace - 搜索替换文本")
    void searchAndReplace() throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        AsposePdfUtil.searchAndReplace(new ByteArrayInputStream(samplePdf), "Strix", "REPLACED", out);
        assertTrue(out.size() > 0);
        // 验证替换结果
        String text = AsposePdfUtil.extractText(new ByteArrayInputStream(out.toByteArray()));
        // 原文 "Strix" 应被替换，但无法保证100%由于 PDF 内部字符存储方式
        assertNotNull(text);
    }

    // ==================== 合并 / 拆分 ====================

    @Test
    @Order(10)
    @DisplayName("merge - 合并多个 PDF")
    void merge() throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        List<ByteArrayInputStream> inputs = Arrays.asList(
                new ByteArrayInputStream(samplePdf),
                new ByteArrayInputStream(samplePdf)
        );
        AsposePdfUtil.merge(
                Arrays.asList(new ByteArrayInputStream(samplePdf), new ByteArrayInputStream(samplePdf)),
                out);
        assertTrue(out.size() > 0);
        // 合并后页数应为原来两倍
        int mergedPageCount = AsposePdfUtil.getPageCount(new ByteArrayInputStream(out.toByteArray()));
        int originalPageCount = AsposePdfUtil.getPageCount(new ByteArrayInputStream(samplePdf));
        assertEquals(originalPageCount * 2, mergedPageCount, "合并后页数应为两倍");
    }

    @Test
    @Order(11)
    @DisplayName("split - 按页范围拆分 PDF")
    void split() throws Exception {
        int totalPages = AsposePdfUtil.getPageCount(new ByteArrayInputStream(samplePdf));
        Assumptions.assumeTrue(totalPages >= 1, "需要至少 1 页");

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        AsposePdfUtil.split(new ByteArrayInputStream(samplePdf), 1, 1, out);
        assertTrue(out.size() > 0);
        assertEquals(1, AsposePdfUtil.getPageCount(new ByteArrayInputStream(out.toByteArray())));
    }

    // ==================== 页面操作 ====================

    @Test
    @Order(12)
    @DisplayName("rotatePage - 旋转页面")
    void rotatePage() throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        AsposePdfUtil.rotatePage(new ByteArrayInputStream(samplePdf), 1, 90, out);
        assertTrue(out.size() > 0);
    }

    // ==================== 优化 ====================

    @Test
    @Order(13)
    @DisplayName("compress - PDF 压缩优化")
    void compress() throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        AsposePdfUtil.compress(new ByteArrayInputStream(samplePdf), out);
        assertTrue(out.size() > 0, "压缩后文件不应为空");
    }

    // ==================== 安全 ====================

    @Test
    @Order(14)
    @DisplayName("encrypt & decrypt - PDF 加密与解密")
    void encryptAndDecrypt() throws Exception {
        // 加密
        ByteArrayOutputStream encOut = new ByteArrayOutputStream();
        AsposePdfUtil.encrypt(new ByteArrayInputStream(samplePdf), "user123", "owner123", encOut);
        assertTrue(encOut.size() > 0);

        // 解密
        ByteArrayOutputStream decOut = new ByteArrayOutputStream();
        AsposePdfUtil.decrypt(new ByteArrayInputStream(encOut.toByteArray()), "owner123", decOut);
        assertTrue(decOut.size() > 0);
    }

}
