package cn.projectan.strix.util.document;

import org.junit.jupiter.api.*;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * AsposeCellsUtil 单元测试
 *
 * @author ProjectAn
 * @since 2026/6/28
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class AsposeCellsUtilTest {

    /**
     * 供多个测试复用的简单 XLSX 字节数组
     */
    private static byte[] sampleWorkbook;

    @BeforeAll
    static void setUp() throws Exception {
        List<List<Object>> data = Arrays.asList(
                Arrays.asList("姓名", "年龄", "邮箱"),
                Arrays.asList("张三", 25, "zhangsan@example.com"),
                Arrays.asList("李四", 30, "lisi@example.com"),
                Arrays.asList("王五", 28, "wangwu@example.com")
        );
        sampleWorkbook = AsposeCellsUtil.createWorkbook(data, "测试数据");
    }

    // ==================== 写入 / 创建 ====================

    @Test
    @Order(1)
    @DisplayName("createWorkbook - 创建基础工作簿")
    void createWorkbook() throws Exception {
        assertNotNull(sampleWorkbook);
        assertTrue(sampleWorkbook.length > 0, "工作簿不应为空");
    }

    @Test
    @Order(2)
    @DisplayName("createWorkbookWithStyle - 创建带样式工作簿")
    void createWorkbookWithStyle() throws Exception {
        List<String> headers = Arrays.asList("商品名称", "单价", "库存");
        List<List<Object>> data = Arrays.asList(
                Arrays.asList("苹果", 5.5, 100),
                Arrays.asList("香蕉", 3.2, 200)
        );
        byte[] result = AsposeCellsUtil.createWorkbookWithStyle("商品列表", headers, data);
        assertNotNull(result);
        assertTrue(result.length > 0);
    }

    // ==================== 读取 ====================

    @Test
    @Order(3)
    @DisplayName("readSheet - 读取工作表数据")
    void readSheet() throws Exception {
        List<List<String>> rows = AsposeCellsUtil.readSheet(new ByteArrayInputStream(sampleWorkbook), 0);
        assertNotNull(rows);
        assertEquals(4, rows.size(), "应有 4 行（1 表头 + 3 数据行）");
        assertEquals("姓名", rows.get(0).get(0));
        assertEquals("张三", rows.get(1).get(0));
    }

    @Test
    @Order(4)
    @DisplayName("readSheetAsMap - 读取工作表为 Map 列表")
    void readSheetAsMap() throws Exception {
        List<Map<String, String>> rows = AsposeCellsUtil.readSheetAsMap(new ByteArrayInputStream(sampleWorkbook), 0);
        assertNotNull(rows);
        assertEquals(3, rows.size(), "应有 3 条数据");
        assertEquals("张三", rows.get(0).get("姓名"));
        assertEquals("lisi@example.com", rows.get(1).get("邮箱"));
    }

    @Test
    @Order(5)
    @DisplayName("getCellValue - 读取指定单元格")
    void getCellValue() throws Exception {
        String value = AsposeCellsUtil.getCellValue(new ByteArrayInputStream(sampleWorkbook), 0, 0, 0);
        assertEquals("姓名", value);
    }

    @Test
    @Order(6)
    @DisplayName("getSheetNames - 获取工作表名称列表")
    void getSheetNames() throws Exception {
        List<String> names = AsposeCellsUtil.getSheetNames(new ByteArrayInputStream(sampleWorkbook));
        assertNotNull(names);
        assertFalse(names.isEmpty());
        assertEquals("测试数据", names.get(0));
    }

    // ==================== 格式转换 ====================

    @Test
    @Order(7)
    @DisplayName("toPdf - Excel 转 PDF")
    void toPdf() throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        AsposeCellsUtil.toPdf(new ByteArrayInputStream(sampleWorkbook), out);
        assertTrue(out.size() > 0, "PDF 不应为空");
        // PDF 文件以 %PDF 开头
        byte[] pdfBytes = out.toByteArray();
        assertEquals('%', (char) pdfBytes[0]);
        assertEquals('P', (char) pdfBytes[1]);
        assertEquals('D', (char) pdfBytes[2]);
        assertEquals('F', (char) pdfBytes[3]);
    }

    @Test
    @Order(8)
    @DisplayName("toHtml - Excel 转 HTML")
    void toHtml() throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        AsposeCellsUtil.toHtml(new ByteArrayInputStream(sampleWorkbook), out);
        assertTrue(out.size() > 0);
        String html = out.toString("UTF-8");
        assertTrue(html.contains("<html") || html.contains("<HTML"), "输出应为合法 HTML");
    }

    @Test
    @Order(9)
    @DisplayName("xlsToCsv - Excel 转 CSV")
    void xlsToCsv() throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        AsposeCellsUtil.xlsToCsv(new ByteArrayInputStream(sampleWorkbook), out);
        assertTrue(out.size() > 0);
        String csv = out.toString("UTF-8");
        assertTrue(csv.contains("姓名"), "CSV 中应包含表头");
        assertTrue(csv.contains("张三"), "CSV 中应包含数据");
    }

    @Test
    @Order(10)
    @DisplayName("toImage - 工作表转图片")
    void toImage() throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        AsposeCellsUtil.toImage(new ByteArrayInputStream(sampleWorkbook), 0, out);
        assertTrue(out.size() > 0, "图片不应为空");
        // PNG 文件头: 0x89 50 4E 47
        byte[] imgBytes = out.toByteArray();
        assertEquals((byte) 0x89, imgBytes[0]);
        assertEquals('P', (char) imgBytes[1]);
        assertEquals('N', (char) imgBytes[2]);
        assertEquals('G', (char) imgBytes[3]);
    }

    // ==================== 模板填充 ====================

    @Test
    @Order(11)
    @DisplayName("fillTemplate - 占位符模板填充")
    void fillTemplate() throws Exception {
        // 创建含占位符的模板
        List<List<Object>> templateData = Arrays.asList(
                Arrays.asList("公司名称：${companyName}", "年份：${year}"),
                Arrays.asList("负责人：${contact}", "金额：${amount}")
        );
        byte[] template = AsposeCellsUtil.createWorkbook(templateData, "模板");

        Map<String, Object> params = new LinkedHashMap<>();
        params.put("companyName", "示例科技有限公司");
        params.put("year", "2026");
        params.put("contact", "张总");
        params.put("amount", "¥100,000");

        byte[] result = AsposeCellsUtil.fillTemplate(new ByteArrayInputStream(template), params);
        assertNotNull(result);
        assertTrue(result.length > 0);

        // 验证填充结果
        List<List<String>> rows = AsposeCellsUtil.readSheet(new ByteArrayInputStream(result), 0);
        assertTrue(rows.get(0).get(0).contains("示例科技有限公司"));
        assertTrue(rows.get(0).get(1).contains("2026"));
    }

    // ==================== 操作 ====================

    @Test
    @Order(12)
    @DisplayName("freezePane - 冻结窗格")
    void freezePane() throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        AsposeCellsUtil.freezePane(new ByteArrayInputStream(sampleWorkbook), 0, 1, 0, out);
        assertTrue(out.size() > 0, "操作后文件不应为空");
    }

    @Test
    @Order(13)
    @DisplayName("mergeCells - 合并单元格")
    void mergeCells() throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        AsposeCellsUtil.mergeCells(new ByteArrayInputStream(sampleWorkbook), 0, 0, 0, 1, 2, out);
        assertTrue(out.size() > 0);
    }

    @Test
    @Order(14)
    @DisplayName("setPassword - 工作簿加密")
    void setPassword() throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        AsposeCellsUtil.setPassword(new ByteArrayInputStream(sampleWorkbook), "Test@2026", out);
        assertTrue(out.size() > 0, "加密后文件不应为空");
    }

}
