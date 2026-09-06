package bvl.export;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Path;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Escribe con XlsWriter y relee el fichero con POI, para comprobar que lo escrito llega al disco.
 */
class XlsWriterTest {

  @TempDir
  Path tmp;

  private File file;

  @BeforeEach
  void copiarPlantilla() {
    // XlsWriter abre un fichero .xls existente, no crea el libro de cero.
    file = tmp.resolve("libro.xls").toFile();
    PlantillaXls.copiarSobre(file);
    assertThat(file).as("plantilla copiada").exists().isNotEmpty();
  }

  private XlsWriter hojaNueva(String nombre) {
    XlsWriter writer = new XlsWriter(file);
    writer.createSheet(nombre);
    writer.setCurrentSheet(nombre);
    return writer;
  }

  @Test
  @DisplayName("los tipos se escriben en la celda que les corresponde")
  void escribeTiposEnSusCeldas() throws IOException {
    XlsWriter writer = hojaNueva("datos");
    writer.addRow();
    writer.addCell("ALICORC1");
    writer.addCell(7.35);
    writer.addCell(1500L);
    writer.addCell(Boolean.TRUE);
    writer.write();

    try (HSSFWorkbook wb = new HSSFWorkbook(new FileInputStream(file))) {
      Row row = wb.getSheet("datos").getRow(0);
      assertThat(row.getCell(0).getStringCellValue()).isEqualTo("ALICORC1");
      assertThat(row.getCell(1).getNumericCellValue()).isEqualTo(7.35);
      // Todo Number acaba como double en la celda.
      assertThat(row.getCell(2).getNumericCellValue()).isEqualTo(1500d);
      assertThat(row.getCell(3).getBooleanCellValue()).isTrue();
    }
  }

  @Test
  @DisplayName("un objeto no soportado se guarda como su toString")
  void objetoNoSoportadoSeGuardaComoTexto() throws IOException {
    XlsWriter writer = hojaNueva("datos");
    writer.addRow();
    writer.addCell(java.time.LocalDate.of(2024, 1, 15));
    writer.write();

    try (HSSFWorkbook wb = new HSSFWorkbook(new FileInputStream(file))) {
      assertThat(wb.getSheet("datos").getRow(0).getCell(0).getStringCellValue())
          .isEqualTo("2024-01-15");
    }
  }

  @Test
  @DisplayName("un valor nulo deja la celda en blanco")
  void valorNuloDejaCeldaEnBlanco() throws IOException {
    XlsWriter writer = hojaNueva("datos");
    writer.addRow();
    writer.addCell(null);
    writer.write();

    try (HSSFWorkbook wb = new HSSFWorkbook(new FileInputStream(file))) {
      Cell cell = wb.getSheet("datos").getRow(0).getCell(0);
      assertThat(cell.getCellType()).isEqualTo(CellType.BLANK);
    }
  }

  @Test
  @DisplayName("addRow avanza de fila y reinicia la columna")
  void addRowAvanzaYReiniciaColumna() throws IOException {
    XlsWriter writer = hojaNueva("datos");
    writer.addRow();
    writer.addCell("fila0col0");
    writer.addCell("fila0col1");
    writer.addRow();
    writer.addCell("fila1col0");
    writer.write();

    try (HSSFWorkbook wb = new HSSFWorkbook(new FileInputStream(file))) {
      Sheet s = wb.getSheet("datos");
      assertThat(s.getRow(0).getCell(0).getStringCellValue()).isEqualTo("fila0col0");
      assertThat(s.getRow(0).getCell(1).getStringCellValue()).isEqualTo("fila0col1");
      assertThat(s.getRow(1).getCell(0).getStringCellValue()).isEqualTo("fila1col0");
    }
  }

  @Test
  @DisplayName("addCell en una columna concreta adelanta el cursor por detras de ella")
  void addCellConIndiceAdelantaElCursor() throws IOException {
    XlsWriter writer = hojaNueva("datos");
    writer.addRow();
    writer.addCell("saltado", (short) 3);
    writer.addCell("siguiente");
    writer.write();

    try (HSSFWorkbook wb = new HSSFWorkbook(new FileInputStream(file))) {
      Row row = wb.getSheet("datos").getRow(0);
      assertThat(row.getCell(3).getStringCellValue()).isEqualTo("saltado");
      assertThat(row.getCell(4).getStringCellValue()).isEqualTo("siguiente");
      assertThat(row.getCell(0)).as("las columnas saltadas quedan sin crear").isNull();
    }
  }

  @Test
  @DisplayName("createSheet sobre una hoja existente la reemplaza, no acumula")
  void createSheetReemplazaLaHojaExistente() throws IOException {
    XlsWriter writer = hojaNueva("datos");
    writer.addRow();
    writer.addCell("viejo");

    writer.createSheet("datos");
    writer.setCurrentSheet("datos");
    writer.addRow();
    writer.addCell("nuevo");
    writer.write();

    try (HSSFWorkbook wb = new HSSFWorkbook(new FileInputStream(file))) {
      // "Hoja1" es la hoja vacia que arrastra /res/template.xls y sobrevive en todo XLS generado.
      assertThat(wb.getNumberOfSheets()).as("no se duplica la hoja recreada").isEqualTo(2);
      assertThat(wb.getSheetName(0)).isEqualTo("Hoja1");
      assertThat(wb.getSheet("datos").getRow(0).getCell(0).getStringCellValue())
          .isEqualTo("nuevo");
    }
  }

  @Test
  @DisplayName("existsSheet distingue hojas creadas de inexistentes")
  void existsSheet() {
    XlsWriter writer = new XlsWriter(file);
    assertThat(writer.existsSheet("datos")).isFalse();
    writer.createSheet("datos");
    assertThat(writer.existsSheet("datos")).isTrue();
  }
}
