package bvl.export;

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

  /** Primera fila de datos: la cabecera ocupa las filas 0, 1 y 2. */
  private static final int PRIMERA_FILA_DATOS = 3;

  @TempDir
  File tmp;

  private String xlsPath() {
    // La raiz se compone con java.io.File; se pasa acabada en separador, como en produccion.
    return tmp.getAbsolutePath() + File.separator;
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

  private File ficheroDiario(String anio, String mes, String nombre) {
    return new File(tmp, anio + File.separator + mes + File.separator + nombre);
  }

  @Test
  @DisplayName("abrirHojaDiaria crea <raiz>/<anio>/<Mes>/<aaaa.mm.dd>.xls con una hoja por hora")
  void layoutDelFicheroDiario() {
    BvlExporter exporter = new BvlExporter(xlsPath());

    exporter.abrirHojaDiaria(LocalDateTime.of(2024, 1, 15, 10, 30, 0));

    assertThat(ficheroDiario("2024", "Enero", "2024.01.15.xls")).exists();
    assertThat(exporter.getLibro().existsSheet("10.30.00")).isTrue();
  }

  @Test
  @DisplayName("abrirHojaMensual crea <raiz>/<anio>/<aaaa.mm_Mes>.xls con una hoja por dia")
  void layoutDelFicheroMensual() {
    BvlExporter exporter = new BvlExporter(xlsPath());

    exporter.abrirHojaMensual(LocalDateTime.of(2024, 1, 15, 10, 30, 0));

    assertThat(new File(tmp, "2024" + File.separator + "2024.01_Enero.xls")).exists();
    assertThat(exporter.getLibro().existsSheet("15")).isTrue();
  }

  @Test
  @DisplayName("los meses de dos digitos y los dias sueltos se rellenan con cero")
  void rellenoDeCerosEnNombres() {
    BvlExporter exporter = new BvlExporter(xlsPath());

    exporter.abrirHojaDiaria(LocalDateTime.of(2024, 10, 5, 9, 5, 3));

    assertThat(ficheroDiario("2024", "Octubre", "2024.10.05.xls")).exists();
    assertThat(exporter.getLibro().existsSheet("09.05.03")).isTrue();
  }

  @Test
  @DisplayName("cada llamada a abrirHojaDiaria agrega una hoja mas al fichero del dia")
  void variasLecturasDelMismoDiaSonVariasHojas() throws IOException {
    BvlExporter exporter = new BvlExporter(xlsPath());
    List<Item> data = List.of(item("ALICORC1", "DIVERSAS", "S/"));

    exporter.abrirHojaDiaria(LocalDateTime.of(2024, 1, 15, 10, 30, 0));
    exporter.escribirDatos(data);
    exporter.abrirHojaDiaria(LocalDateTime.of(2024, 1, 15, 10, 50, 0));
    exporter.escribirDatos(data);

    try (HSSFWorkbook wb = new HSSFWorkbook(
        new FileInputStream(ficheroDiario("2024", "Enero", "2024.01.15.xls")))) {
      assertThat(wb.getSheet("10.30.00")).isNotNull();
      assertThat(wb.getSheet("10.50.00")).isNotNull();
    }
  }

  @Test
  @DisplayName("escribirDatos coloca cada dato en su columna")
  void escribirDatosColocaCadaDatoEnSuColumna() throws IOException {
    BvlExporter exporter = new BvlExporter(xlsPath());
    exporter.abrirHojaDiaria(LocalDateTime.of(2024, 1, 15, 10, 30, 0));

    exporter.escribirDatos(List.of(item("ALICORC1", "DIVERSAS", "S/")));

    try (HSSFWorkbook wb = new HSSFWorkbook(
        new FileInputStream(ficheroDiario("2024", "Enero", "2024.01.15.xls")))) {
      Row row = wb.getSheet("10.30.00").getRow(PRIMERA_FILA_DATOS);
      assertThat(row.getCell(ColumnaCotizacion.NEMONICO.indice()).getStringCellValue())
          .isEqualTo("ALICORC1");
      assertThat(row.getCell(ColumnaCotizacion.ANTERIOR.indice()).getNumericCellValue())
          .isEqualTo(7.10);
      assertThat(row.getCell(ColumnaCotizacion.FECHA_ANTERIOR.indice()).getStringCellValue())
          .isEqualTo("2024-01-12");
      assertThat(row.getCell(ColumnaCotizacion.APERTURA.indice()).getNumericCellValue())
          .isEqualTo(7.25);
      // La columna EX-DER no se calcula: siempre lleva el relleno literal.
      assertThat(row.getCell(ColumnaCotizacion.EX_DERECHO.indice()).getStringCellValue())
          .isEqualTo("-------");
      assertThat(row.getCell(ColumnaCotizacion.ULTIMA.indice()).getNumericCellValue())
          .isEqualTo(7.35);
      assertThat(row.getCell(ColumnaCotizacion.COMPRA.indice()).getNumericCellValue())
          .isEqualTo(7.30);
      assertThat(row.getCell(ColumnaCotizacion.VENTA.indice()).getNumericCellValue())
          .isEqualTo(7.40);
      assertThat(row.getCell(ColumnaCotizacion.NUMERO_ACCIONES.indice()).getNumericCellValue())
          .isEqualTo(15000d);
      assertThat(row.getCell(ColumnaCotizacion.MONTO_NEGOCIADO.indice()).getNumericCellValue())
          .isEqualTo(110250d);
      assertThat(row.getCell(ColumnaCotizacion.NUMERO_OPERACIONES.indice()).getNumericCellValue())
          .isEqualTo(42d);
      assertThat(row.getCell(ColumnaCotizacion.MONEDA.indice()).getStringCellValue())
          .isEqualTo("S/");
      assertThat(row.getCell(ColumnaCotizacion.SECTOR.indice()).getStringCellValue())
          .isEqualTo("DIVERSAS");
      assertThat(row.getCell(ColumnaCotizacion.VARIACION.indice()).getNumericCellValue())
          .isEqualTo(3.52);
    }
  }

  @Test
  @DisplayName("varios items se escriben en filas consecutivas bajo la cabecera")
  void variosItemsEnFilasConsecutivas() throws IOException {
    BvlExporter exporter = new BvlExporter(xlsPath());
    exporter.abrirHojaDiaria(LocalDateTime.of(2024, 1, 15, 10, 30, 0));

    exporter.escribirDatos(List.of(
        item("ALICORC1", "DIVERSAS", "S/"),
        item("BAP", "BANCOS", "US$"),
        item("VOLCABC1", "MINERAS", "S/")));

    try (HSSFWorkbook wb = new HSSFWorkbook(
        new FileInputStream(ficheroDiario("2024", "Enero", "2024.01.15.xls")))) {
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
    exporter.abrirHojaDiaria(LocalDateTime.of(2024, 1, 15, 10, 30, 0));
    exporter.escribirDatos(List.of(item("ALICORC1", "DIVERSAS", "S/")));

    try (HSSFWorkbook wb = new HSSFWorkbook(
        new FileInputStream(ficheroDiario("2024", "Enero", "2024.01.15.xls")))) {
      Sheet s = wb.getSheet("10.30.00");
      assertThat(s.getRow(0).getCell(0).getStringCellValue())
          .isEqualTo("2024-01-15 10:30:00");
      assertThat(s.getRow(1).getCell(0).getStringCellValue()).isEqualTo("ACCIONES");
      assertThat(s.getRow(1).getCell(1).getStringCellValue()).isEqualTo("PRECIOS");
      assertThat(s.getRow(2).getCell(1).getStringCellValue()).isEqualTo("ANT");
      assertThat(s.getRow(2).getCell(5).getStringCellValue()).isEqualTo("ULT");
    }
  }

  @Test
  @DisplayName("la cabecera coloca cada grupo y subtitulo en su columna")
  void cabeceraCompleta() throws IOException {
    // La maqueta de la cabecera se declara en CabeceraCotizaciones; esto la fija sobre el fichero.
    BvlExporter exporter = new BvlExporter(xlsPath());
    exporter.abrirHojaDiaria(LocalDateTime.of(2024, 1, 15, 10, 30, 0));
    exporter.escribirDatos(List.of(item("ALICORC1", "DIVERSAS", "S/")));

    try (HSSFWorkbook wb = new HSSFWorkbook(
        new FileInputStream(ficheroDiario("2024", "Enero", "2024.01.15.xls")))) {
      Sheet s = wb.getSheet("10.30.00");
      Row grupos = s.getRow(1);
      assertThat(grupos.getCell(6).getStringCellValue()).isEqualTo("PROPUESTAS");
      assertThat(grupos.getCell(8).getStringCellValue()).isEqualTo("No.ACC o GRUPOS");
      assertThat(grupos.getCell(9).getStringCellValue()).isEqualTo("MONTO NEGOCIADO");
      assertThat(grupos.getCell(10).getStringCellValue()).isEqualTo("No. OPE.");
      assertThat(grupos.getCell(11).getStringCellValue()).isEqualTo("MON EDA");
      assertThat(grupos.getCell(12).getStringCellValue()).isEqualTo("SEC TOR");
      assertThat(grupos.getCell(13).getStringCellValue()).isEqualTo("VARIA CION");

      Row subtitulos = s.getRow(2);
      assertThat(subtitulos.getCell(2).getStringCellValue()).isEqualTo("A.FEC");
      assertThat(subtitulos.getCell(3).getStringCellValue()).isEqualTo("APE");
      assertThat(subtitulos.getCell(4).getStringCellValue()).isEqualTo("EX-DER");
      assertThat(subtitulos.getCell(6).getStringCellValue()).isEqualTo("COM");
      assertThat(subtitulos.getCell(7).getStringCellValue()).isEqualTo("VEN");

      // ACCIONES ocupa las dos filas de cabecera; PRECIOS solo la suya, de la 1 a la 5.
      assertThat(s.getMergedRegions()).anySatisfy(r -> {
        assertThat(r.getFirstRow()).isEqualTo(1);
        assertThat(r.getLastRow()).isEqualTo(2);
        assertThat(r.getFirstColumn()).isZero();
      });
      assertThat(s.getMergedRegions()).anySatisfy(r -> {
        assertThat(r.getFirstRow()).isEqualTo(1);
        assertThat(r.getLastRow()).isEqualTo(1);
        assertThat(r.getFirstColumn()).isEqualTo(1);
        assertThat(r.getLastColumn()).isEqualTo(5);
      });
    }
  }
}
