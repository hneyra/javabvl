package bvl.service;

import static org.assertj.core.api.Assertions.assertThat;

import bvl.config.BvlProperties;
import bvl.domain.Accion;
import bvl.domain.Item;
import bvl.domain.Moneda;
import bvl.domain.Sector;
import bvl.schedule.Lectura;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Sheet;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * {@link ExportService} escribiendo los dos ficheros de una lectura sobre un directorio temporal.
 *
 * <p>Cubre el paso de exportar desde la base a exportar desde memoria. Antes las cotizaciones se
 * guardaban en H2 y se releian para volcarlas, lo que arrastraba a la hoja del dia los items
 * duplicados de ciclos anteriores; {@link #dosCiclosConLaMismaFechaNoDuplicanFilas()} fija que eso
 * ya no ocurre.
 */
class ExportServiceIntegrationTest {

  /** Cabecera en las filas 0, 1 y 2; escribirDatos deja una fila vacia tras el ultimo item. */
  private static final int PRIMERA_FILA_DATOS = 3;

  @TempDir
  File tmp;

  private ExportService servicio() {
    BvlProperties propiedades = new BvlProperties("http://localhost", "http://localhost/market",
        "http://localhost/daily", tmp.getAbsolutePath() + File.separator, "2", "00:10:00",
        "9:40:00", "16:30", "00:05:00");
    return new ExportService(propiedades);
  }

  private static Item item(String nemonico, LocalDateTime fecha) {
    Sector s = new Sector();
    s.setNombre("DIVERSAS");
    Accion a = new Accion();
    a.setNemonico(nemonico);
    a.setEmpresa("Empresa " + nemonico);
    a.setSector(s);
    Moneda m = new Moneda();
    m.setNombre("S/");
    Item i = new Item();
    i.setAccion(a);
    i.setMoneda(m);
    i.setSegmento("");
    i.setFechaLectura(fecha);
    i.setCotizacionUltima(7.35);
    i.setVariacionPorcentual(1.5);
    return i;
  }

  private File diario() {
    return new File(tmp, "2024" + File.separator + "Enero" + File.separator + "2024.01.15.xls");
  }

  private File mensual() {
    return new File(tmp, "2024" + File.separator + "2024.01_Enero.xls");
  }

  private int filasDeDatos(File fichero, String hoja) throws IOException {
    try (HSSFWorkbook wb = new HSSFWorkbook(new FileInputStream(fichero))) {
      Sheet s = wb.getSheet(hoja);
      return s.getLastRowNum() - PRIMERA_FILA_DATOS;
    }
  }

  @Test
  @DisplayName("una lectura produce el fichero del dia y el del mes, cada uno con su hoja")
  void exportaLosDosFicheros() {
    LocalDateTime fecha = LocalDateTime.of(2024, 1, 15, 10, 30);

    servicio().exportar(new Lectura(List.of(item("ALICORC1", fecha)), fecha));

    assertThat(diario()).exists();
    assertThat(mensual()).exists();
  }

  @Test
  @DisplayName("cada cotizacion leida es una fila, en los dos ficheros")
  void escribeUnaFilaPorCotizacion() throws IOException {
    LocalDateTime fecha = LocalDateTime.of(2024, 1, 15, 10, 30);
    List<Item> items = List.of(
        item("ALICORC1", fecha), item("BAP", fecha), item("VOLCABC1", fecha));

    servicio().exportar(new Lectura(items, fecha));

    assertThat(filasDeDatos(diario(), "10.30.00")).isEqualTo(3);
    assertThat(filasDeDatos(mensual(), "15")).isEqualTo(3);
  }

  @Test
  @DisplayName("dos sondeos de la misma hora se sobrescriben, no se acumulan")
  void dosCiclosConLaMismaFechaNoDuplicanFilas() throws IOException {
    // Ocurre cuando la BVL devuelve el mismo updatedDate en dos sondeos seguidos. Mientras la
    // exportacion releia de la base, cada repeticion sumaba una copia de cada cotizacion.
    LocalDateTime fecha = LocalDateTime.of(2024, 1, 15, 10, 30);
    ExportService servicio = servicio();
    Lectura resultado = new Lectura(List.of(item("ALICORC1", fecha)), fecha);

    servicio.exportar(resultado);
    servicio.exportar(resultado);

    assertThat(filasDeDatos(diario(), "10.30.00")).isEqualTo(1);
  }

  @Test
  @DisplayName("dos horas del mismo dia son dos hojas del fichero diario")
  void dosHorasSonDosHojas() throws IOException {
    LocalDateTime primera = LocalDateTime.of(2024, 1, 15, 10, 30);
    LocalDateTime segunda = LocalDateTime.of(2024, 1, 15, 10, 50);
    ExportService servicio = servicio();

    servicio.exportar(new Lectura(List.of(item("ALICORC1", primera)), primera));
    servicio.exportar(new Lectura(List.of(item("ALICORC1", segunda)), segunda));

    try (HSSFWorkbook wb = new HSSFWorkbook(new FileInputStream(diario()))) {
      assertThat(wb.getSheet("10.30.00")).isNotNull();
      assertThat(wb.getSheet("10.50.00")).isNotNull();
    }
  }

  @Test
  @DisplayName("una lectura sin cotizaciones crea la hoja vacia y no revienta")
  void lecturaVacia() {
    LocalDateTime fecha = LocalDateTime.of(2024, 1, 15, 10, 30);

    servicio().exportar(new Lectura(List.of(), fecha));

    assertThat(diario()).exists();
    assertThat(mensual()).exists();
  }
}
