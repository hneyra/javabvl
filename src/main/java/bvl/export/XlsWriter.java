package bvl.export;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Calendar;
import java.util.Date;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.hssf.util.HSSFColor;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.ss.util.RegionUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Escritura sobre un libro {@code .xls} con un cursor: hay una hoja actual, una fila actual y una
 * columna actual, y {@link #addCell(Object)} avanza sola.
 *
 * <p>El libro se abre a partir de un fichero que <b>ya existe</b> (la plantilla copiada por
 * {@link PlantillaXls}); esta clase no crea libros de cero.
 *
 * <p>El estilo funciona por acumulacion: {@link #nuevoEstilo()} abre uno, los metodos
 * {@code conXxx} le van anadiendo rasgos y {@link #aplicarEstilo()} lo pega a la ultima celda
 * escrita. Mientras no se abra otro, cada {@code aplicarEstilo()} reutiliza el mismo, que es como
 * las cabeceras comparten formato sin repetir la receta.
 */
public class XlsWriter {

    private static final Logger logger = LoggerFactory.getLogger(XlsWriter.class);

    private final File file;

    private Workbook workbook;
    private CellStyle style;
    private Sheet currentSheet;
    private Row currentRow;
    private Cell currentCell;
    private CellRangeAddress currentRegion;
    private short currentColumnPos;
    private short currentRowPos;

    public XlsWriter(File file) {
        this.file = file;
        try (InputStream in = new FileInputStream(file)) {
            workbook = new HSSFWorkbook(in);
        } catch (Exception e) {
            logger.error("No se pudo abrir el libro {}: {}", file, e.getMessage(), e);
        }
    }

    /**
     * TRAMPA CONOCIDA: no hace nada, asi que los {@link Workbook} que abre esta clase nunca se
     * cierran. Se conserva tal cual; ver "Trampas conocidas" en CLAUDE.md.
     */
    public void closeResources() {
        // Intencionadamente vacio.
    }

    /** Crea la hoja. Si ya existia la reemplaza, para no acumular duplicados con el mismo nombre. */
    public void createSheet(String sheetname) {
        int sheetIndex = workbook.getSheetIndex(sheetname);
        if (sheetIndex > -1) {
            logger.info("La hoja {} ya existia (index={}), se crea de nuevo", sheetname, sheetIndex);
            workbook.removeSheetAt(sheetIndex);
        }
        workbook.createSheet(sheetname);
    }

    public boolean existsSheet(String sheetname) {
        return workbook.getSheet(sheetname) != null;
    }

    /** Situa el cursor al final de la hoja indicada, listo para seguir escribiendo debajo. */
    public void setCurrentSheet(String name) {
        currentSheet = workbook.getSheet(name);
        currentRowPos = (short) currentSheet.getLastRowNum();
        Row ultima = currentSheet.getRow(currentRowPos);
        currentColumnPos = ultima == null ? 0 : ultima.getLastCellNum();
        logger.debug("Hoja actual={}, columna={}, fila={}", name, currentColumnPos, currentRowPos);
    }

    /** Baja una fila y vuelve a la primera columna. */
    public void addRow() {
        createRow(++currentRowPos);
    }

    public void createRow(short pos) {
        currentRow = currentSheet.createRow(pos);
        currentRowPos = (short) Math.max(pos, currentRowPos);
        currentColumnPos = 0;
    }

    /** Escribe en la columna actual y avanza el cursor. */
    public void addCell(Object value) {
        addCell(value, currentColumnPos);
    }

    /** Escribe en una columna concreta; el cursor salta detras de ella si iba por detras. */
    public void addCell(Object value, short index) {
        if (currentRow == null) {
            logger.error("No hay fila actual: no se pudo agregar la celda {}", value);
            return;
        }
        currentCell = currentRow.createCell(index);
        setCellValue(currentCell, value);
        if (currentColumnPos <= index) {
            currentColumnPos = (short) (index + 1);
        }
    }

    public void mergeCell(int rowFrom, short colFrom, int rowTo, short colTo) {
        mergeCell(new CellRangeAddress(rowFrom, rowTo, colFrom, colTo));
    }

    public void mergeCell(CellRangeAddress region) {
        currentRegion = region;
        currentSheet.addMergedRegion(region);
    }

    /** Ancho de la columna de la ultima celda escrita. */
    public void setColumnWidth(short width) {
        currentSheet.setColumnWidth(currentColumnPos - 1, width);
    }

    // --- Estilos -------------------------------------------------------------------------------

    /** Abre un estilo nuevo. Los {@code conXxx} siguientes lo modifican. */
    public void nuevoEstilo() {
        style = workbook.createCellStyle();
    }

    /** Pega el estilo abierto a la ultima celda escrita. */
    public void aplicarEstilo() {
        currentCell.setCellStyle(style);
    }

    /** Texto centrado en ambos ejes y ajustado al ancho de la celda. */
    public void conCentradoYAjuste() {
        style.setWrapText(true);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        style.setAlignment(HorizontalAlignment.CENTER);
    }

    /** Letra de cabecera: negrita y blanca, para leerse sobre los fondos oscuros. */
    public void conLetraDeCabecera() {
        Font font = workbook.createFont();
        font.setColor(HSSFColor.HSSFColorPredefined.WHITE.getIndex());
        font.setBold(true);
        style.setFont(font);
    }

    /** Fondo solido del color indicado y recuadro medio blanco en los cuatro lados. */
    public void conFondoYBordes(short indexColor) {
        style.setFillForegroundColor(indexColor);
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);

        short blanco = HSSFColor.HSSFColorPredefined.WHITE.getIndex();
        style.setBottomBorderColor(blanco);
        style.setTopBorderColor(blanco);
        style.setLeftBorderColor(blanco);
        style.setRightBorderColor(blanco);
        style.setBorderBottom(BorderStyle.MEDIUM);
        style.setBorderTop(BorderStyle.MEDIUM);
        style.setBorderLeft(BorderStyle.MEDIUM);
        style.setBorderRight(BorderStyle.MEDIUM);
    }

    /**
     * Recuadra la ultima region fusionada. Va aparte de {@link #conFondoYBordes(short)} porque un
     * estilo de celda no puede dibujar el borde exterior de un bloque de varias celdas.
     */
    public void bordearFusion(int indexColor) {
        try {
            RegionUtil.setBottomBorderColor(indexColor, currentRegion, currentSheet);
            RegionUtil.setTopBorderColor(indexColor, currentRegion, currentSheet);
            RegionUtil.setLeftBorderColor(indexColor, currentRegion, currentSheet);
            RegionUtil.setRightBorderColor(indexColor, currentRegion, currentSheet);
            RegionUtil.setBorderBottom(BorderStyle.MEDIUM, currentRegion, currentSheet);
            RegionUtil.setBorderTop(BorderStyle.MEDIUM, currentRegion, currentSheet);
            RegionUtil.setBorderLeft(BorderStyle.MEDIUM, currentRegion, currentSheet);
            RegionUtil.setBorderRight(BorderStyle.MEDIUM, currentRegion, currentSheet);
        } catch (Exception e) {
            logger.error("No se pudo recuadrar la region fusionada: {}", e.getMessage(), e);
        }
    }

    // --- Volcado -------------------------------------------------------------------------------

    public void write() {
        try (OutputStream out = new FileOutputStream(file)) {
            workbook.write(out);
        } catch (IOException e) {
            logger.error("No se pudo escribir el libro {}: {}", file, e.getMessage(), e);
        }
    }

    private static void setCellValue(Cell cell, Object value) {
        switch (value) {
            // Deja la celda BLANK, que es como se representa un dato que la BVL no publica.
            case null -> cell.setCellValue((String) null);
            case Date d -> cell.setCellValue(d);
            case Calendar c -> cell.setCellValue(c);
            case Boolean b -> cell.setCellValue(b);
            // Todo numero acaba como double: es el unico tipo numerico que entiende el formato.
            case Number n -> cell.setCellValue(n.doubleValue());
            default -> cell.setCellValue(String.valueOf(value));
        }
    }

    public short getCurrentColumnPos() {
        return currentColumnPos;
    }
}
