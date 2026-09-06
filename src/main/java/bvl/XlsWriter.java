package bvl;

/*
 * XLSWriter.java
 *
 * Created on 5 de agosto de 2005, 07:20 AM
 */

import org.apache.poi.hssf.usermodel.HSSFCellStyle;
import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.hssf.util.HSSFColor;
import org.apache.poi.poifs.filesystem.POIFSFileSystem;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.ss.util.RegionUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.Calendar;
import java.util.Date;

/**
 * @author jneyra
 */
public class XlsWriter {

    Logger logger = LoggerFactory.getLogger(getClass());

    // Workbook workbook;
    File file;

    Workbook workbook;
    CellStyle style;
    private Sheet currentSheet;
    private Row currentRow;
    private Cell currentCell;
    private CellRangeAddress currentRegion;
    private short currentColumnPos;
    private short currentRowPos;

    // OPCPackage pkg = null;

    /**
     * Creates a new instance of XLSWriter
     */

    public XlsWriter(File file) {
        this.file = file;
        try {
            // pkg = OPCPackage.open(file);
            workbook = new HSSFWorkbook(new FileInputStream(file));
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
    }

    public static void main(String[] args) throws IOException {
        POIFSFileSystem fs = new POIFSFileSystem(new FileInputStream("F:\\workbook.xls"));
        HSSFWorkbook wb = new HSSFWorkbook(fs);
        // HSSFWorkbook wb = new HSSFWorkbook();
        HSSFSheet sheet = wb.createSheet("sheet2");
        HSSFRow row = sheet.createRow((short) 2);
        row.createCell(0).setCellValue(1.1);
        row.createCell(1).setCellValue(new Date());
        row.createCell(2).setCellValue("a string");
        row.createCell(3).setCellValue(true);
        row.createCell(4).setCellType(CellType.ERROR);

        // Write the output to a file
        FileOutputStream fileOut = new FileOutputStream("res/template.xls");
        wb.write(fileOut);
        fileOut.close();
        wb.close();
    }

    public void closeResources() {
        // if (pkg != null) {
        // try {
        // pkg.close();
        // } catch (IOException e) {
        // e.printStackTrace();
        // }
        // }
    }

    public void createSheet(String sheetname) {
        int sheetIndex = workbook.getSheetIndex(sheetname);

        if (sheetIndex > -1) {
            System.out.println(
                    "La hoja: " + sheetname + " ya existía (index=" + sheetIndex
                            + "), será creada nuevamente.");
            workbook.removeSheetAt(sheetIndex);
        }
        workbook.createSheet(sheetname);
    }

    public boolean existsSheet(String sheetname) {
        return null != workbook.getSheet(sheetname);
    }

    public void createCellStyle() {
        style = workbook.createCellStyle();
    }

    public void addRow() {
        createRow(++currentRowPos);
    }

    public void createRow(short pos) {
        currentRow = currentSheet.createRow(pos);
        currentRowPos = (pos > currentRowPos ? pos : currentRowPos);
        currentColumnPos = 0;
    }

    public void setCurrentRow(int num) {
        currentRow = currentSheet.getRow(num);
        currentColumnPos = currentRow.getLastCellNum();
    }

    public void setCurrentSheet(String name) {
        currentSheet = workbook.getSheet(name);
        currentRowPos = (short) currentSheet.getLastRowNum(); // TODO
        // currentRowPos
        // debe ser int
        if (currentSheet.getRow(currentRowPos) == null) {
            currentColumnPos = 0;
        } else {
            currentColumnPos = currentSheet.getRow(currentRowPos).getLastCellNum();
        }
        logger.info(
                "Setted new sheet=" + name + ", colpos=" + currentColumnPos + ", rowpos=" + currentRowPos);
    }

    public void setCurrentSheet(int index) {
        currentSheet = workbook.getSheetAt(index);
    }

    /**
     * Agrega una nueva celda a la hoja actual
     *
     * @param value valor que se agregará a la hoja actual.
     */
    public void addCell(Object value) {
        addCell(value, currentColumnPos);
    }

    /**
     * Agrega una nueva celda a la hoja actual
     *
     * @param value valor que se agregará a la hoja actual.
     */
    public void addCell(Object value, short index) {
        if (currentRow == null) {
            logger.error("currentRow is null. No se logró agregar la celda: " + value);
            return;
        }
        currentCell = currentRow.createCell(index);
        setCellValue(currentCell, value);
        if (currentColumnPos <= index) {
            currentColumnPos = index;
            currentColumnPos++;
        }
        // System.out.println("Cell addedd:" + value + " at " + index
        // + ", curCol = " + currentColumnPos);
    }

    public void mergeCell(int rowFrom, short colFrom, int rowTo, short colTo) {
        currentRegion = new CellRangeAddress(rowFrom, rowTo, colFrom, colTo);
        mergeCell(currentRegion);
    }

    public void mergeCell(CellRangeAddress region) {
        currentSheet.addMergedRegion(currentRegion = region);
    }

    public void setCellAlignment(HorizontalAlignment align) {
        Cell cell = currentRow.getCell(currentColumnPos - 1);
        try {
            CellStyle cellStyle = workbook.createCellStyle();
            cell.setCellStyle(cellStyle);
            cellStyle.setAlignment(align);
        } catch (Exception ne) {
            ne.printStackTrace();
        }
    }

    public void setColumnWidth(short width) {
        currentSheet.setColumnWidth((short) (currentColumnPos - 1), width);
    }

    public void setCellStyle(HSSFCellStyle style) {
        Cell cell = currentRow.getCell(currentColumnPos - 1);
        cell.setCellStyle(style);
    }

    public void useDefaultStyle() {

    }

    public void applyStyle() {
        currentCell.setCellStyle(style);
    }

    public void useStyleAlign() {
        style.setWrapText(true);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        style.setAlignment(HorizontalAlignment.CENTER);
    }

    public void useStyleAlign(HorizontalAlignment align) {
        style.setAlignment(align);
    }

    public void useStyle2() {
        Font font = workbook.createFont();
        font.setColor(HSSFColor.HSSFColorPredefined.WHITE.getIndex());
        font.setBold(true);
        style.setFont(font);
    }

    public void useStyle3(short indexColor) {
        style.setFillForegroundColor(indexColor);
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);

        style.setBottomBorderColor(HSSFColor.HSSFColorPredefined.WHITE.getIndex());
        style.setTopBorderColor(HSSFColor.HSSFColorPredefined.WHITE.getIndex());
        style.setLeftBorderColor(HSSFColor.HSSFColorPredefined.WHITE.getIndex());
        style.setRightBorderColor(HSSFColor.HSSFColorPredefined.WHITE.getIndex());
        style.setBorderBottom(BorderStyle.MEDIUM);
        style.setBorderTop(BorderStyle.MEDIUM);
        style.setBorderLeft(BorderStyle.MEDIUM);
        style.setBorderRight(BorderStyle.MEDIUM);
    }

    public void useMergedStyle(int indexColor) {
        try {
            RegionUtil.setBottomBorderColor(indexColor, currentRegion, currentSheet);
            RegionUtil.setTopBorderColor(indexColor, currentRegion, currentSheet);
            RegionUtil.setLeftBorderColor(indexColor, currentRegion, currentSheet);
            RegionUtil.setRightBorderColor(indexColor, currentRegion, currentSheet);
            RegionUtil
                    .setBorderBottom(BorderStyle.MEDIUM, currentRegion, currentSheet);
            RegionUtil.setBorderTop(BorderStyle.MEDIUM, currentRegion, currentSheet);
            RegionUtil.setBorderLeft(BorderStyle.MEDIUM, currentRegion, currentSheet);
            RegionUtil.setBorderRight(BorderStyle.MEDIUM, currentRegion, currentSheet);
        } catch (Exception ne) {
            logger.error(ne.getMessage(), ne);
        }
    }

    public void write() {
        try {
            workbook.write(new FileOutputStream(file));
        } catch (FileNotFoundException fnfe) {
            fnfe.printStackTrace();
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
    }

    public void writeAs(File out) {
        try {
            workbook.write(new FileOutputStream(out));
        } catch (FileNotFoundException fnfe) {
            fnfe.printStackTrace();
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
    }

    private void setCellValue(Cell cell, Object value) {
        if (value instanceof Date) {
            cell.setCellValue((Date) value);
        } else if (value instanceof Calendar) {
            cell.setCellValue((Calendar) value);
        } else if (value instanceof Boolean) {
            cell.setCellValue(((Boolean) value).booleanValue());
        } else if (value instanceof Number) {
            cell.setCellValue(((Number) value).doubleValue());
        } else {
            cell.setCellValue(value == null ? null : String.valueOf(value));
        }
    }

    public short getCurrentColumnPos() {
        return currentColumnPos;
    }

    public void setCurrentColumnPos(short currentColumnPos) {
        this.currentColumnPos = currentColumnPos;
    }

    public short getCurrentRowPos() {
        return currentRowPos;
    }

    public void setCurrentRowPos(short currentRowPos) {
        this.currentRowPos = currentRowPos;
    }

}
