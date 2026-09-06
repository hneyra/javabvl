package bvl;

import static org.assertj.core.api.Assertions.assertThat;

import bvl.domain.Accion;
import bvl.domain.Item;
import bvl.domain.Moneda;
import bvl.domain.Sector;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * {@link BvlExporter} escribiendo XLS de verdad sobre un directorio temporal.
 *
 * <p>Fija el layout de carpetas y hojas del que depende el usuario final, y la posicion de cada
 * columna dentro de la fila de datos.
 */
class BvlExporterIntegrationTest {

  /** Primera fila de datos: createSheet deja escritas tres filas de cabecera (0, 1 y 2). */
  private static final int PRIMERA_FILA_DATOS = 3;

  @TempDir
  File tmp;

  private String xlsPath() {
    // BvlExporter concatena directamente, la ruta tiene que acabar en separador.
    return tmp.getAbsolutePath() + File.separator;
  }

  private static Date fecha(LocalDateTime ldt) {
    return Date.from(ldt.atZone(ZoneId.systemDefault()).toInstant());
  }

  private static Item item(String nemonico, String sector, String moneda) {
    Sector s = new Sector();
    s.setNombre(sector);

    Accion a = new Accion();
    a.setNemonico(nemonico);
    a.setEmpresa("Empresa " + nemonico);
    a.setSector(s);

    Moneda m = new Moneda();
    m.setNombre(moneda);

    Item i = new Item();
    i.setAccion(a);
    i.setMoneda(m);
    i.setCotizacionAnterior(7.10);
    i.setFechaAnterior(LocalDate.of(2024, 1, 12));
    i.setCotizacionApertura(7.25);
    i.setCotizacionUltima(7.35);
    i.setPropuestaCompra(7.30);
    i.setPropuestaVenta(7.40);
    i.setNumeroAcciones(15000L);
    i.setMontoNegociado(110250L);
    i.setNumeroOperaciones(42L);
    i.setVariacionPorcentual(3.52);
    return i;
  }

  @Test
  @DisplayName("createXLSDay crea <raiz>/<anio>/<Mes>/<aaaa.mm.dd>.xls con una hoja por hora")
  void layoutDelFicheroDiario() {
    BvlExporter exporter = new BvlExporter(xlsPath());

    exporter.createXLSDay(fecha(LocalDateTime.of(2024, 1, 15, 10, 30, 0)));

    File esperado = new File(tmp, "2024" + File.separator + "Enero" + File.separator
        + "2024.01.15.xls");
    assertThat(esperado).exists();
    assertThat(exporter.getXls().existsSheet("10.30.00")).isTrue();
  }

  @Test
  @DisplayName("createXlsMonth crea <raiz>/<anio>/<aaaa.mm_Mes>.xls con una hoja por dia")
  void layoutDelFicheroMensual() {
    BvlExporter exporter = new BvlExporter(xlsPath());

    exporter.createXlsMonth(fecha(LocalDateTime.of(2024, 1, 15, 10, 30, 0)));

    File esperado = new File(tmp, "2024" + File.separator + "2024.01_Enero.xls");
    assertThat(esperado).exists();
    assertThat(exporter.getXls().existsSheet("15")).isTrue();
  }

  @Test
  @DisplayName("los meses de dos digitos y los dias sueltos se rellenan con cero")
  void relenoDeCerosEnNombres() {
    BvlExporter exporter = new BvlExporter(xlsPath());

    exporter.createXLSDay(fecha(LocalDateTime.of(2024, 10, 5, 9, 5, 3)));

    assertThat(new File(tmp, "2024" + File.separator + "Octubre" + File.separator
        + "2024.10.05.xls")).exists();
    assertThat(exporter.getXls().existsSheet("09.05.03")).isTrue();
  }

  @Test
  @DisplayName("cada llamada a createXLSDay agrega una hoja mas al fichero del dia")
  void variasLecturasDelMismoDiaSonVariasHojas() throws IOException {
    BvlExporter exporter = new BvlExporter(xlsPath());
    List<Item> data = List.of(item("ALICORC1", "DIVERSAS", "S/"));

    exporter.createXLSDay(fecha(LocalDateTime.of(2024, 1, 15, 10, 30, 0)));
    exporter.writeData(data);
    exporter.createXLSDay(fecha(LocalDateTime.of(2024, 1, 15, 10, 50, 0)));
    exporter.writeData(data);

    File file = new File(tmp, "2024" + File.separator + "Enero" + File.separator
        + "2024.01.15.xls");
    try (HSSFWorkbook wb = new HSSFWorkbook(new FileInputStream(file))) {
      assertThat(wb.getSheet("10.30.00")).isNotNull();
      assertThat(wb.getSheet("10.50.00")).isNotNull();
    }
  }

  @Test
  @DisplayName("writeData coloca cada dato en su columna")
  void writeDataColocaCadaDatoEnSuColumna() throws IOException {
    BvlExporter exporter = new BvlExporter(xlsPath());
    exporter.createXLSDay(fecha(LocalDateTime.of(2024, 1, 15, 10, 30, 0)));

    exporter.writeData(List.of(item("ALICORC1", "DIVERSAS", "S/")));

    File file = new File(tmp, "2024" + File.separator + "Enero" + File.separator
        + "2024.01.15.xls");
    try (HSSFWorkbook wb = new HSSFWorkbook(new FileInputStream(file))) {
      Row row = wb.getSheet("10.30.00").getRow(PRIMERA_FILA_DATOS);
      assertThat(row.getCell(0).getStringCellValue()).isEqualTo("ALICORC1");
      assertThat(row.getCell(1).getNumericCellValue()).isEqualTo(7.10);
      assertThat(row.getCell(2).getStringCellValue()).isEqualTo("2024-01-12");
      assertThat(row.getCell(3).getNumericCellValue()).isEqualTo(7.25);
      // La columna EX-DER no se calcula: siempre lleva el relleno literal.
      assertThat(row.getCell(4).getStringCellValue()).isEqualTo("-------");
      assertThat(row.getCell(5).getNumericCellValue()).isEqualTo(7.35);
      assertThat(row.getCell(6).getNumericCellValue()).isEqualTo(7.30);
      assertThat(row.getCell(7).getNumericCellValue()).isEqualTo(7.40);
      assertThat(row.getCell(8).getNumericCellValue()).isEqualTo(15000d);
      assertThat(row.getCell(9).getNumericCellValue()).isEqualTo(110250d);
      assertThat(row.getCell(10).getNumericCellValue()).isEqualTo(42d);
      assertThat(row.getCell(11).getStringCellValue()).isEqualTo("S/");
      assertThat(row.getCell(12).getStringCellValue()).isEqualTo("DIVERSAS");
      assertThat(row.getCell(13).getNumericCellValue()).isEqualTo(3.52);
    }
  }

  @Test
  @DisplayName("varios items se escriben en filas consecutivas bajo la cabecera")
  void variosItemsEnFilasConsecutivas() throws IOException {
    BvlExporter exporter = new BvlExporter(xlsPath());
    exporter.createXLSDay(fecha(LocalDateTime.of(2024, 1, 15, 10, 30, 0)));

    exporter.writeData(List.of(
        item("ALICORC1", "DIVERSAS", "S/"),
        item("BAP", "BANCOS", "US$"),
        item("VOLCABC1", "MINERAS", "S/")));

    File file = new File(tmp, "2024" + File.separator + "Enero" + File.separator
        + "2024.01.15.xls");
    try (HSSFWorkbook wb = new HSSFWorkbook(new FileInputStream(file))) {
      Sheet s = wb.getSheet("10.30.00");
      assertThat(s.getRow(PRIMERA_FILA_DATOS).getCell(0).getStringCellValue())
          .isEqualTo("ALICORC1");
      assertThat(s.getRow(PRIMERA_FILA_DATOS + 1).getCell(0).getStringCellValue())
          .isEqualTo("BAP");
      assertThat(s.getRow(PRIMERA_FILA_DATOS + 2).getCell(0).getStringCellValue())
          .isEqualTo("VOLCABC1");
    }
  }

  @Test
  @DisplayName("la hoja lleva la fecha de lectura y las cabeceras de la plantilla")
  void cabeceraDeLaHoja() throws IOException {
    BvlExporter exporter = new BvlExporter(xlsPath());
    exporter.createXLSDay(fecha(LocalDateTime.of(2024, 1, 15, 10, 30, 0)));
    exporter.writeData(List.of(item("ALICORC1", "DIVERSAS", "S/")));

    File file = new File(tmp, "2024" + File.separator + "Enero" + File.separator
        + "2024.01.15.xls");
    try (HSSFWorkbook wb = new HSSFWorkbook(new FileInputStream(file))) {
      Sheet s = wb.getSheet("10.30.00");
      assertThat(s.getRow(0).getCell(0).getStringCellValue())
          .isEqualTo("2024-01-15 10:30:00");
      assertThat(s.getRow(1).getCell(0).getStringCellValue()).isEqualTo("ACCIONES");
      assertThat(s.getRow(1).getCell(1).getStringCellValue()).isEqualTo("PRECIOS");
      assertThat(s.getRow(2).getCell(1).getStringCellValue()).isEqualTo("ANT");
      assertThat(s.getRow(2).getCell(5).getStringCellValue()).isEqualTo("ULT");
    }
  }
}
