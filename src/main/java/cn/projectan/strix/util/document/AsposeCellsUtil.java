package cn.projectan.strix.util.document;

import cn.projectan.strix.core.exception.StrixException;
import com.aspose.cells.*;
import lombok.extern.slf4j.Slf4j;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Aspose.Cells 工具类（Excel 处理）
 * <p>
 * 支持格式转换、读取、写入、模板填充、单元格操作等功能。
 * 所有方法均为静态方法，无需实例化。
 * </p>
 *
 * @author ProjectAn
 * @since 2026/6/28 12:23
 */
@Slf4j
public class AsposeCellsUtil {

    static {
        init();
    }

    private static void init() {
        try {
            InputStream is = AsposeCellsUtil.class.getResourceAsStream("/other/license.xml");
            if (is == null) {
                log.error("[Strix Document Cells] 未找到授权文件, 初始化失败.");
                return;
            }
            License license = new License();
            license.setLicense(is);
            log.info("[Strix Document Cells] 授权文件加载成功.");
        } catch (Exception e) {
            log.error("[Strix Document Cells] 授权文件加载失败.", e);
            throw new StrixException("Strix Document Cells 授权文件加载失败: " + e.getMessage(), e);
        }
    }

    // ==================== 格式转换 ====================

    /**
     * Excel（XLS/XLSX）转 PDF
     *
     * @param excel Excel 输入流
     * @param out   PDF 输出流
     */
    public static void toPdf(InputStream excel, OutputStream out) throws Exception {
        Workbook workbook = new Workbook(excel);
        workbook.save(out, SaveFormat.PDF);
    }

    /**
     * Excel 转 HTML（图片以 Base64 内嵌，输出单文件）
     *
     * @param excel Excel 输入流
     * @param out   HTML 输出流
     */
    public static void toHtml(InputStream excel, OutputStream out) throws Exception {
        Workbook workbook = new Workbook(excel);
        HtmlSaveOptions options = new HtmlSaveOptions(SaveFormat.HTML);
        options.setExportImagesAsBase64(true);
        workbook.save(out, options);
    }

    /**
     * 指定工作表转图片（PNG）
     *
     * @param excel      Excel 输入流
     * @param sheetIndex 工作表下标（0-based）
     * @param out        图片输出流
     */
    public static void toImage(InputStream excel, int sheetIndex, OutputStream out) throws Exception {
        Workbook workbook = new Workbook(excel);
        Worksheet worksheet = workbook.getWorksheets().get(sheetIndex);
        ImageOrPrintOptions imgOptions = new ImageOrPrintOptions();
        imgOptions.setImageType(ImageType.PNG);
        imgOptions.setOnePagePerSheet(true);
        SheetRender render = new SheetRender(worksheet, imgOptions);
        render.toImage(0, out);
    }

    /**
     * 全部工作表转图片并打包为 ZIP
     * <p>
     * 每张工作表生成一个 PNG 文件，命名格式：sheet_01_表名.png。空工作表自动跳过。
     * </p>
     *
     * @param excel Excel 输入流
     * @param out   ZIP 压缩包输出流
     */
    public static void toImages(InputStream excel, OutputStream out) throws Exception {
        toImages(excel, out, null);
    }

    /**
     * 全部工作表转图片并打包为 ZIP，支持逐表进度回调
     *
     * @param excel            Excel 输入流
     * @param out              ZIP 压缩包输出流
     * @param progressCallback 进度回调，接收已处理的工作表序号（1-based），为 null 时忽略
     */
    public static void toImages(InputStream excel, OutputStream out,
                                java.util.function.IntConsumer progressCallback) throws Exception {
        Workbook workbook = new Workbook(excel);
        WorksheetCollection sheets = workbook.getWorksheets();
        ImageOrPrintOptions imgOptions = new ImageOrPrintOptions();
        imgOptions.setImageType(ImageType.PNG);
        imgOptions.setOnePagePerSheet(true);
        int processed = 0;
        int totalValidSheets = 0;
        // Pre-count valid sheets for accurate progress
        for (int s = 0; s < sheets.getCount(); s++) {
            Cells cells = sheets.get(s).getCells();
            if (cells.getMaxDataRow() >= 0 || cells.getMaxDataColumn() >= 0) {
                totalValidSheets++;
            }
        }
        try (ZipOutputStream zipOut = new ZipOutputStream(out)) {
            for (int s = 0; s < sheets.getCount(); s++) {
                Worksheet sheet = sheets.get(s);
                Cells cells = sheet.getCells();
                if (cells.getMaxDataRow() < 0 && cells.getMaxDataColumn() < 0) {
                    continue;
                }
                SheetRender render = new SheetRender(sheet, imgOptions);
                ByteArrayOutputStream imgOut = new ByteArrayOutputStream();
                render.toImage(0, imgOut);
                if (imgOut.size() < 512) {
                    continue;
                }
                String entryName = String.format("sheet_%02d_%s.png", s + 1, sheet.getName());
                zipOut.putNextEntry(new ZipEntry(entryName));
                zipOut.write(imgOut.toByteArray());
                zipOut.closeEntry();
                processed++;
                if (progressCallback != null) progressCallback.accept(processed);
            }
        }
    }

    /**
     * Excel 转 CSV（导出第一个工作表）
     *
     * @param excel Excel 输入流
     * @param out   CSV 输出流
     */
    public static void xlsToCsv(InputStream excel, OutputStream out) throws Exception {
        Workbook workbook = new Workbook(excel);
        TxtSaveOptions txtOptions = new TxtSaveOptions(SaveFormat.CSV);
        workbook.save(out, txtOptions);
    }

    // ==================== 读取 ====================

    /**
     * 读取指定工作表的所有数据
     *
     * @param excel      Excel 输入流
     * @param sheetIndex 工作表下标（0-based）
     * @return 二维列表，外层为行，内层为列（全部为字符串）
     */
    public static List<List<String>> readSheet(InputStream excel, int sheetIndex) throws Exception {
        Workbook workbook = new Workbook(excel);
        Worksheet worksheet = workbook.getWorksheets().get(sheetIndex);
        Cells cells = worksheet.getCells();
        int maxRow = cells.getMaxDataRow();
        int maxCol = cells.getMaxDataColumn();
        List<List<String>> result = new ArrayList<>();
        for (int row = 0; row <= maxRow; row++) {
            List<String> rowData = new ArrayList<>();
            for (int col = 0; col <= maxCol; col++) {
                Cell cell = cells.get(row, col);
                rowData.add(cell != null ? cell.getStringValue() : "");
            }
            result.add(rowData);
        }
        return result;
    }

    /**
     * 读取指定工作表为 Map 列表（首行作为 Header）
     *
     * @param excel      Excel 输入流
     * @param sheetIndex 工作表下标（0-based）
     * @return List&lt;Map&lt;Header, Value&gt;&gt;，顺序保留
     */
    public static List<Map<String, String>> readSheetAsMap(InputStream excel, int sheetIndex) throws Exception {
        Workbook workbook = new Workbook(excel);
        Worksheet worksheet = workbook.getWorksheets().get(sheetIndex);
        Cells cells = worksheet.getCells();
        int maxRow = cells.getMaxDataRow();
        int maxCol = cells.getMaxDataColumn();

        List<String> headers = new ArrayList<>();
        for (int col = 0; col <= maxCol; col++) {
            Cell cell = cells.get(0, col);
            headers.add(cell != null && !cell.getStringValue().isBlank() ? cell.getStringValue() : "col" + col);
        }

        List<Map<String, String>> result = new ArrayList<>();
        for (int row = 1; row <= maxRow; row++) {
            Map<String, String> rowMap = new LinkedHashMap<>();
            for (int col = 0; col <= maxCol; col++) {
                Cell cell = cells.get(row, col);
                rowMap.put(headers.get(col), cell != null ? cell.getStringValue() : "");
            }
            result.add(rowMap);
        }
        return result;
    }

    /**
     * 读取指定单元格的字符串值
     *
     * @param excel      Excel 输入流
     * @param sheetIndex 工作表下标（0-based）
     * @param row        行下标（0-based）
     * @param col        列下标（0-based）
     * @return 单元格字符串值，单元格为空时返回 null
     */
    public static String getCellValue(InputStream excel, int sheetIndex, int row, int col) throws Exception {
        Workbook workbook = new Workbook(excel);
        Worksheet worksheet = workbook.getWorksheets().get(sheetIndex);
        Cell cell = worksheet.getCells().get(row, col);
        return cell != null ? cell.getStringValue() : null;
    }

    /**
     * 获取工作簿中所有工作表名称
     *
     * @param excel Excel 输入流
     * @return 工作表名称列表
     */
    public static List<String> getSheetNames(InputStream excel) throws Exception {
        Workbook workbook = new Workbook(excel);
        List<String> names = new ArrayList<>();
        WorksheetCollection sheets = workbook.getWorksheets();
        for (int i = 0; i < sheets.getCount(); i++) {
            names.add(sheets.get(i).getName());
        }
        return names;
    }

    // ==================== 写入 / 创建 ====================

    /**
     * 从二维数据创建 Excel 工作簿（无样式）
     *
     * @param data      二维数据，外层行内层列
     * @param sheetName 工作表名称
     * @return XLSX 格式的字节数组
     */
    public static byte[] createWorkbook(List<List<Object>> data, String sheetName) throws Exception {
        Workbook workbook = new Workbook();
        Worksheet worksheet = workbook.getWorksheets().get(0);
        worksheet.setName(sheetName);
        Cells cells = worksheet.getCells();
        for (int row = 0; row < data.size(); row++) {
            List<Object> rowData = data.get(row);
            for (int col = 0; col < rowData.size(); col++) {
                cells.get(row, col).putValue(rowData.get(col));
            }
        }
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        workbook.save(baos, SaveFormat.XLSX);
        return baos.toByteArray();
    }

    /**
     * 创建带样式的 Excel 工作簿（首行加粗蓝色背景，自动调整列宽）
     *
     * @param sheetName 工作表名称
     * @param headers   表头列表
     * @param data      数据行列表
     * @return XLSX 格式的字节数组
     */
    public static byte[] createWorkbookWithStyle(String sheetName, List<String> headers, List<List<Object>> data) throws Exception {
        Workbook workbook = new Workbook();
        Worksheet worksheet = workbook.getWorksheets().get(0);
        worksheet.setName(sheetName);
        Cells cells = worksheet.getCells();

        Style headerStyle = workbook.createStyle();
        headerStyle.getFont().setBold(true);
        headerStyle.getFont().setSize(11);
        headerStyle.setForegroundColor(Color.fromArgb(217, 225, 242));
        headerStyle.setPattern(BackgroundType.SOLID);
        headerStyle.setHorizontalAlignment(TextAlignmentType.CENTER);

        for (int col = 0; col < headers.size(); col++) {
            Cell cell = cells.get(0, col);
            cell.putValue(headers.get(col));
            cell.setStyle(headerStyle);
        }

        for (int row = 0; row < data.size(); row++) {
            List<Object> rowData = data.get(row);
            for (int col = 0; col < rowData.size(); col++) {
                cells.get(row + 1, col).putValue(rowData.get(col));
            }
        }

        worksheet.autoFitColumns();

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        workbook.save(baos, SaveFormat.XLSX);
        return baos.toByteArray();
    }

    /**
     * 基于模板填充数据（占位符格式：{@code ${key}}）
     * <p>
     * 遍历所有工作表中的字符串单元格，将其中的 {@code ${key}} 替换为对应值。
     * </p>
     *
     * @param template 模板 Excel 输入流
     * @param data     键值对数据，key 对应占位符名称
     * @return 填充后的 XLSX 字节数组
     */
    public static byte[] fillTemplate(InputStream template, Map<String, Object> data) throws Exception {
        Workbook workbook = new Workbook(template);
        WorksheetCollection sheets = workbook.getWorksheets();
        for (int s = 0; s < sheets.getCount(); s++) {
            Cells cells = sheets.get(s).getCells();
            int maxRow = cells.getMaxDataRow();
            int maxCol = cells.getMaxDataColumn();
            for (int row = 0; row <= maxRow; row++) {
                for (int col = 0; col <= maxCol; col++) {
                    Cell cell = cells.get(row, col);
                    if (cell != null && cell.getType() == CellValueType.IS_STRING) {
                        String value = cell.getStringValue();
                        boolean changed = false;
                        for (Map.Entry<String, Object> entry : data.entrySet()) {
                            String placeholder = "${" + entry.getKey() + "}";
                            if (value.contains(placeholder)) {
                                value = value.replace(placeholder, String.valueOf(entry.getValue()));
                                changed = true;
                            }
                        }
                        if (changed) {
                            cell.putValue(value);
                        }
                    }
                }
            }
        }
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        workbook.save(baos, SaveFormat.XLSX);
        return baos.toByteArray();
    }

    // ==================== 单元格操作 ====================

    /**
     * 合并指定区域的单元格
     *
     * @param excel      Excel 输入流
     * @param sheetIndex 工作表下标（0-based）
     * @param firstRow   起始行（0-based）
     * @param firstCol   起始列（0-based）
     * @param totalRows  跨越行数
     * @param totalCols  跨越列数
     * @param out        输出流
     */
    public static void mergeCells(InputStream excel, int sheetIndex,
                                  int firstRow, int firstCol, int totalRows, int totalCols,
                                  OutputStream out) throws Exception {
        Workbook workbook = new Workbook(excel);
        Worksheet worksheet = workbook.getWorksheets().get(sheetIndex);
        worksheet.getCells().merge(firstRow, firstCol, totalRows, totalCols);
        workbook.save(out, SaveFormat.XLSX);
    }

    /**
     * 冻结指定行列（窗格冻结）
     *
     * @param excel      Excel 输入流
     * @param sheetIndex 工作表下标（0-based）
     * @param row        冻结至第几行（0-based，该行及上方将被冻结）
     * @param col        冻结至第几列（0-based，该列及左侧将被冻结）
     * @param out        输出流
     */
    public static void freezePane(InputStream excel, int sheetIndex, int row, int col, OutputStream out) throws Exception {
        Workbook workbook = new Workbook(excel);
        Worksheet worksheet = workbook.getWorksheets().get(sheetIndex);
        worksheet.freezePanes(row, col, row, col);
        workbook.save(out, SaveFormat.XLSX);
    }

    /**
     * 对工作簿设置打开密码保护
     *
     * @param excel    Excel 输入流
     * @param password 密码
     * @param out      输出流
     */
    public static void setPassword(InputStream excel, String password, OutputStream out) throws Exception {
        Workbook workbook = new Workbook(excel);
        workbook.getSettings().setPassword(password);
        workbook.save(out, SaveFormat.XLSX);
    }

}
