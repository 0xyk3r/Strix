package cn.projectan.strix.util.document;

import com.aspose.words.Document;
import com.aspose.words.DocumentBuilder;
import com.aspose.words.SaveFormat;
import org.junit.jupiter.api.*;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * AsposeWordsUtil 单元测试
 *
 * @author ProjectAn
 * @since 2026/6/28
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class AsposeWordsUtilTest {

    /**
     * 供多个测试复用的简单 DOCX 字节数组
     */
    private static byte[] sampleDocx;

    @BeforeAll
    static void setUp() throws Exception {
        // 用 Aspose.Words API 创建一个简单文档
        Document doc = new Document();
        DocumentBuilder builder = new DocumentBuilder(doc);
        builder.writeln("Strix 文档转换测试");
        builder.writeln("第一段落：本文档由 Aspose.Words 自动生成，用于单元测试。");
        builder.writeln("第二段落：支持 PDF 转换、邮件合并、书签填充等功能。");
        builder.writeln("English paragraph: This is for testing purposes.");

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        doc.save(baos, SaveFormat.DOCX);
        sampleDocx = baos.toByteArray();
    }

    // ==================== 读取 ====================

    @Test
    @Order(1)
    @DisplayName("extractText - 提取全文文本")
    void extractText() throws Exception {
        String text = AsposeWordsUtil.extractText(new ByteArrayInputStream(sampleDocx));
        assertNotNull(text);
        assertFalse(text.isBlank());
        assertTrue(text.contains("Strix") || text.contains("文档转换"), "应包含原始内容");
    }

    @Test
    @Order(2)
    @DisplayName("getPageCount - 获取总页数")
    void getPageCount() throws Exception {
        int count = AsposeWordsUtil.getPageCount(new ByteArrayInputStream(sampleDocx));
        assertTrue(count >= 1, "页数应至少为 1");
    }

    // ==================== 格式转换 ====================

    @Test
    @Order(3)
    @DisplayName("toPdf - DOCX 转 PDF")
    void toPdf() throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        AsposeWordsUtil.toPdf(new ByteArrayInputStream(sampleDocx), out);
        assertTrue(out.size() > 0, "PDF 不应为空");
        byte[] pdfBytes = out.toByteArray();
        assertEquals('%', (char) pdfBytes[0]);
        assertEquals('P', (char) pdfBytes[1]);
        assertEquals('D', (char) pdfBytes[2]);
        assertEquals('F', (char) pdfBytes[3]);
    }

    @Test
    @Order(4)
    @DisplayName("toHtml - DOCX 转 HTML")
    void toHtml() throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        AsposeWordsUtil.toHtml(new ByteArrayInputStream(sampleDocx), out);
        assertTrue(out.size() > 0);
        String html = out.toString("UTF-8");
        assertTrue(html.contains("<html") || html.contains("<HTML"));
    }

    @Test
    @Order(5)
    @DisplayName("toMarkdown - DOCX 转 Markdown")
    void toMarkdown() throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        AsposeWordsUtil.toMarkdown(new ByteArrayInputStream(sampleDocx), out);
        assertTrue(out.size() > 0);
    }

    @Test
    @Order(6)
    @DisplayName("toImage - 指定页面转图片")
    void toImage() throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        AsposeWordsUtil.toImage(new ByteArrayInputStream(sampleDocx), 0, out);
        assertTrue(out.size() > 0, "图片不应为空");
        // PNG 文件头
        byte[] imgBytes = out.toByteArray();
        assertEquals((byte) 0x89, imgBytes[0]);
        assertEquals('P', (char) imgBytes[1]);
        assertEquals('N', (char) imgBytes[2]);
        assertEquals('G', (char) imgBytes[3]);
    }

    @Test
    @Order(7)
    @DisplayName("htmlToDocx - HTML 字符串转 DOCX")
    void htmlToDocx() throws Exception {
        String html = "<html><body><h1>测试标题</h1><p>这是一个段落。</p></body></html>";
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        AsposeWordsUtil.htmlToDocx(html, out);
        assertTrue(out.size() > 0);
        String text = AsposeWordsUtil.extractText(new ByteArrayInputStream(out.toByteArray()));
        assertTrue(text.contains("测试标题"), "应包含原始 HTML 内容");
    }

    // ==================== 模板 / 邮件合并 ====================

    @Test
    @Order(8)
    @DisplayName("mailMerge - 简单邮件合并")
    void mailMerge() throws Exception {
        // 创建带合并域的模板
        Document template = new Document();
        DocumentBuilder builder = new DocumentBuilder(template);
        builder.write("尊敬的 ");
        builder.insertField("MERGEFIELD name");
        builder.writeln("：");
        builder.write("您的账户 ");
        builder.insertField("MERGEFIELD account");
        builder.writeln(" 已创建成功。");

        ByteArrayOutputStream templateBaos = new ByteArrayOutputStream();
        template.save(templateBaos, SaveFormat.DOCX);

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("name", "张三");
        data.put("account", "zhangsan@strix.com");

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        AsposeWordsUtil.mailMerge(new ByteArrayInputStream(templateBaos.toByteArray()), data, out);
        assertTrue(out.size() > 0);

        String result = AsposeWordsUtil.extractText(new ByteArrayInputStream(out.toByteArray()));
        assertTrue(result.contains("张三"), "合并结果应包含姓名");
        assertTrue(result.contains("zhangsan@strix.com"), "合并结果应包含账户");
    }

    @Test
    @Order(9)
    @DisplayName("fillBookmarks - 书签填充")
    void fillBookmarks() throws Exception {
        // 创建带书签的模板
        Document template = new Document();
        DocumentBuilder builder = new DocumentBuilder(template);
        builder.write("合同编号：");
        builder.startBookmark("contractNo");
        builder.write("待填充");
        builder.endBookmark("contractNo");
        builder.writeln();
        builder.write("签署日期：");
        builder.startBookmark("signDate");
        builder.write("待填充");
        builder.endBookmark("signDate");

        ByteArrayOutputStream templateBaos = new ByteArrayOutputStream();
        template.save(templateBaos, SaveFormat.DOCX);

        Map<String, String> bookmarks = new LinkedHashMap<>();
        bookmarks.put("contractNo", "HT-2026-001");
        bookmarks.put("signDate", "2026年6月28日");

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        AsposeWordsUtil.fillBookmarks(
                new ByteArrayInputStream(templateBaos.toByteArray()), bookmarks, out);
        assertTrue(out.size() > 0);

        String result = AsposeWordsUtil.extractText(new ByteArrayInputStream(out.toByteArray()));
        assertTrue(result.contains("HT-2026-001"), "应包含合同编号");
        assertTrue(result.contains("2026年6月28日"), "应包含签署日期");
    }

    // ==================== 内容操作 ====================

    @Test
    @Order(10)
    @DisplayName("findAndReplace - 查找替换文本")
    void findAndReplace() throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        AsposeWordsUtil.findAndReplace(new ByteArrayInputStream(sampleDocx), "Strix", "REPLACED", out);
        assertTrue(out.size() > 0);
        String result = AsposeWordsUtil.extractText(new ByteArrayInputStream(out.toByteArray()));
        assertTrue(result.contains("REPLACED"), "替换后应包含新文本");
    }

    @Test
    @Order(11)
    @DisplayName("addWatermark - 添加文字水印")
    void addWatermark() throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        AsposeWordsUtil.addWatermark(new ByteArrayInputStream(sampleDocx), "机密文件", out);
        assertTrue(out.size() > 0);
    }

    @Test
    @Order(12)
    @DisplayName("insertHeader & insertFooter - 设置页眉页脚")
    void insertHeaderAndFooter() throws Exception {
        ByteArrayOutputStream headerOut = new ByteArrayOutputStream();
        AsposeWordsUtil.insertHeader(new ByteArrayInputStream(sampleDocx), "Strix 系统文档", headerOut);
        assertTrue(headerOut.size() > 0);

        ByteArrayOutputStream footerOut = new ByteArrayOutputStream();
        AsposeWordsUtil.insertFooter(new ByteArrayInputStream(sampleDocx), "Strix © 2026", true, footerOut);
        assertTrue(footerOut.size() > 0);
    }

    // ==================== 合并 / 拆分 ====================

    @Test
    @Order(13)
    @DisplayName("merge - 合并多个 Word 文档")
    void merge() throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        AsposeWordsUtil.merge(
                Arrays.asList(
                        new ByteArrayInputStream(sampleDocx),
                        new ByteArrayInputStream(sampleDocx)
                ), out);
        assertTrue(out.size() > 0);
        int mergedPages = AsposeWordsUtil.getPageCount(new ByteArrayInputStream(out.toByteArray()));
        int originalPages = AsposeWordsUtil.getPageCount(new ByteArrayInputStream(sampleDocx));
        assertTrue(mergedPages >= originalPages, "合并后页数应不少于原始页数");
    }

    // ==================== 保护 ====================

    @Test
    @Order(14)
    @DisplayName("protect & removeProtection - 文档保护与解除")
    void protectAndRemoveProtection() throws Exception {
        ByteArrayOutputStream protectedOut = new ByteArrayOutputStream();
        AsposeWordsUtil.protect(new ByteArrayInputStream(sampleDocx), "Test@2026", protectedOut);
        assertTrue(protectedOut.size() > 0);

        ByteArrayOutputStream unprotectedOut = new ByteArrayOutputStream();
        AsposeWordsUtil.removeProtection(
                new ByteArrayInputStream(protectedOut.toByteArray()), "Test@2026", unprotectedOut);
        assertTrue(unprotectedOut.size() > 0);
    }

    // ==================== 其他 ====================

    @Test
    @Order(15)
    @DisplayName("acceptAllRevisions - 接受所有修订")
    void acceptAllRevisions() throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        AsposeWordsUtil.acceptAllRevisions(new ByteArrayInputStream(sampleDocx), out);
        assertTrue(out.size() > 0);
    }

    @Test
    @Order(16)
    @DisplayName("updateFields - 更新域（含目录）")
    void updateFields() throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        AsposeWordsUtil.updateFields(new ByteArrayInputStream(sampleDocx), out);
        assertTrue(out.size() > 0);
    }

}
