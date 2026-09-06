package bvl.service;

import static org.assertj.core.api.Assertions.assertThat;

import bvl.config.BvlProperties;
import bvl.domain.Accion;
import bvl.domain.Item;
import bvl.domain.Lectura;
import bvl.domain.Moneda;
import bvl.domain.Sector;
import bvl.support.TestJpaConfig;
import java.io.File;
import java.io.FileInputStream;
import java.time.LocalDateTime;
import java.util.List;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ContextConfiguration;

/**
 * El puente entre la duplicacion en la base y lo que acaba viendo el usuario en el XLS.
 *
 * <p>Los demas tests miran una cosa o la otra; este junta {@link LecturaService} y
 * {@link ExportService} sobre una H2 real y un directorio temporal, que es como corren en
 * produccion dentro de un ciclo de sondeo.
 */
@DataJpaTest
@ContextConfiguration(classes = TestJpaConfig.class)
@Import({CatalogoService.class, LecturaService.class})
class ExportDuplicadosTest {

  /**
   * Cabecera: filas 0, 1 y 2; los datos empiezan en la 3. Tras el ultimo item queda una fila vacia
   * de mas, porque escribirDatos hace addRow() despues de cada uno; de ahi que el numero de filas
   * de datos sea exactamente {@code getLastRowNum() - PRIMERA_FILA_DATOS}.
   */
  private static final int PRIMERA_FILA_DATOS = 3;

  @Autowired LecturaService lecturas;
  @Autowired TestEntityManager em;
  @TempDir File tmp;

  private static Item item(LocalDateTime fecha) {
    Sector s = new Sector(); s.setNombre("DIVERSAS");
    Accion a = new Accion(); a.setNemonico("ALICORC1"); a.setEmpresa("Alicorp"); a.setSector(s);
    Moneda m = new Moneda(); m.setNombre("S/");
    Lectura l = new Lectura(); l.setFecha(fecha);
    Item i = new Item();
    i.setAccion(a); i.setMoneda(m); i.setLectura(l); i.setSegmento("");
    i.setFechaLectura(fecha); i.setVariacionPorcentual(1.5);
    return i;
  }

  @Test
  @DisplayName("CARACTERIZACION: dos ciclos con la misma fecha duplican las filas del XLS")
  void dosCiclosConLaMismaFechaDuplicanFilasEnElXls() throws Exception {
    // Consecuencia visible de que saveData no tenga guarda anti-duplicado: exportar relee de la
    // BD lo que acaba de escribir, encuentra los items repetidos y rehace la hoja con todos.
    // Pasa cuando la BVL devuelve el mismo updatedDate en dos sondeos seguidos.
    LocalDateTime fecha = LocalDateTime.of(2024, 1, 15, 10, 30);
    BvlProperties props = new BvlProperties("u", "u", "u",
        tmp.getAbsolutePath() + File.separator, "2", "00:10:00", "9:40:00", "16:30", "00:05:00");
    ExportService export = new ExportService(lecturas, props);

    lecturas.saveData(List.of(item(fecha)), fecha);
    em.flush();
    export.exportar(fecha, fecha);
    int filasPrimerCiclo = filasDeDatos(fecha);

    // Segundo sondeo con la MISMA fecha publicada: la BVL no ha publicado nada nuevo.
    lecturas.saveData(List.of(item(fecha)), fecha);
    em.flush();
    export.exportar(fecha, fecha);
    int filasSegundoCiclo = filasDeDatos(fecha);

    assertThat(filasPrimerCiclo).as("una cotizacion, una fila").isEqualTo(1);
    assertThat(filasSegundoCiclo).as("la misma cotizacion, repetida").isEqualTo(2);
  }

  private int filasDeDatos(LocalDateTime fecha) throws Exception {
    File f = new File(tmp, "2024" + File.separator + "Enero" + File.separator + "2024.01.15.xls");
    try (HSSFWorkbook wb = new HSSFWorkbook(new FileInputStream(f))) {
      return wb.getSheet("10.30.00").getLastRowNum() - PRIMERA_FILA_DATOS;
    }
  }
}
