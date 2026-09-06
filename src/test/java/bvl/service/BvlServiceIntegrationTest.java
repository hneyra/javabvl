package bvl.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import bvl.domain.Accion;
import bvl.domain.Item;
import bvl.domain.Lectura;
import bvl.domain.Moneda;
import bvl.domain.Sector;
import bvl.repository.AccionRepository;
import bvl.repository.ItemRepository;
import bvl.repository.LecturaRepository;
import bvl.repository.MonedaRepository;
import bvl.repository.SectorRepository;
import bvl.support.TestJpaConfig;
import java.time.DateTimeException;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;

/**
 * {@link BvlService} contra una H2 real: es donde se decide que se deduplica y que se duplica en
 * cada ciclo de sondeo.
 *
 * <p>Varios tests caracterizan comportamiento actual que contradice lo que sugiere el nombre del
 * metodo. Estan marcados como CARACTERIZACION y explicados; ver la seccion "Trampas conocidas"
 * de CLAUDE.md.
 *
 * <p>Sin clases {@code @Nested}: el surefire 2.22.2 que fija Boot 3.0.2 no las selecciona con
 * {@code -Dtest=}.
 */
@DataJpaTest
@ContextConfiguration(classes = TestJpaConfig.class)
@Import(BvlService.class)
@TestPropertySource(properties = "xlsPath=${java.io.tmpdir}/javabvl-tests/")
class BvlServiceIntegrationTest {

  @Autowired
  BvlService service;

  @Autowired
  TestEntityManager em;

  @Autowired
  LecturaRepository lecturaRepository;

  @Autowired
  AccionRepository accionRepository;

  @Autowired
  ItemRepository itemRepository;

  @Autowired
  MonedaRepository monedaRepository;

  @Autowired
  SectorRepository sectorRepository;

  /** Construye un Item transitorio tal y como lo entrega BvlReader.fromBvlItem. */
  private static Item itemLeido(String nemonico, String sector, String moneda,
      LocalDateTime fechaLectura, Double variacion) {
    Sector s = new Sector();
    s.setNombre(sector);

    Accion a = new Accion();
    a.setNemonico(nemonico);
    a.setEmpresa("Empresa " + nemonico);
    a.setSector(s);

    Moneda m = new Moneda();
    m.setNombre(moneda);

    Lectura l = new Lectura();
    l.setFecha(fechaLectura);

    Item i = new Item();
    i.setAccion(a);
    i.setMoneda(m);
    i.setLectura(l);
    i.setSegmento("");
    i.setFechaLectura(fechaLectura);
    i.setVariacionPorcentual(variacion);
    return i;
  }

  @Test
  @DisplayName("saveData persiste los items y crea una sola fila por clave natural")
  void saveDataPersisteYDeduplicaCatalogos() {
    LocalDateTime fecha = LocalDateTime.of(2024, 1, 15, 10, 0);
    List<Item> data = List.of(
        itemLeido("ALICORC1", "Diversas", "Soles", fecha, 1.5),
        itemLeido("BAP", "Bancos", "Soles", fecha, -2.0));

    service.saveData(data, fecha);
    em.flush();

    assertThat(itemRepository.count()).isEqualTo(2);
    assertThat(accionRepository.count()).isEqualTo(2);
    assertThat(sectorRepository.count()).isEqualTo(2);
    // Las dos acciones comparten moneda y lectura: una fila de cada una.
    assertThat(monedaRepository.count()).isEqualTo(1);
    assertThat(lecturaRepository.count()).isEqualTo(1);
  }

  @Test
  @DisplayName("saveData en un segundo ciclo reutiliza accion, sector, moneda y lectura")
  void saveDataSegundoCicloReutilizaLosCatalogos() {
    LocalDateTime fecha = LocalDateTime.of(2024, 1, 15, 10, 0);

    service.saveData(List.of(itemLeido("ALICORC1", "Diversas", "Soles", fecha, 1.5)), fecha);
    em.flush();
    Long idAccion = accionRepository.findByNemonico("ALICORC1").getId();

    service.saveData(List.of(itemLeido("ALICORC1", "Diversas", "Soles", fecha, 1.5)), fecha);
    em.flush();

    assertThat(accionRepository.count()).isEqualTo(1);
    assertThat(accionRepository.findByNemonico("ALICORC1").getId()).isEqualTo(idAccion);
    assertThat(sectorRepository.count()).isEqualTo(1);
    assertThat(monedaRepository.count()).isEqualTo(1);
    assertThat(lecturaRepository.count()).isEqualTo(1);
  }

  @Test
  @DisplayName("CARACTERIZACION: repetir la misma lectura duplica los items")
  void saveDataRepetirLaMismaLecturaDuplicaItems() {
    // La guarda "if (lastDate == null || !fecha.equals(lastDate))" esta comentada en saveData,
    // igual que la de BVL2.process(). Cada ciclo reinserta los items aunque la BVL no haya
    // publicado nada nuevo, todos colgando de la misma Lectura.
    LocalDateTime fecha = LocalDateTime.of(2024, 1, 15, 10, 0);

    service.saveData(List.of(itemLeido("ALICORC1", "Diversas", "Soles", fecha, 1.5)), fecha);
    em.flush();
    service.saveData(List.of(itemLeido("ALICORC1", "Diversas", "Soles", fecha, 1.5)), fecha);
    em.flush();

    assertThat(itemRepository.count()).as("items duplicados por ciclo repetido").isEqualTo(2);
    assertThat(lecturaRepository.count()).as("sobre una unica lectura").isEqualTo(1);
  }

  @Test
  @DisplayName("saveData mantiene separadas dos lecturas distintas del mismo dia")
  void saveDataDosLecturasDistintasConviven() {
    LocalDateTime t1 = LocalDateTime.of(2024, 1, 15, 10, 0);
    LocalDateTime t2 = LocalDateTime.of(2024, 1, 15, 10, 20);

    service.saveData(List.of(itemLeido("ALICORC1", "Diversas", "Soles", t1, 1.5)), t1);
    service.saveData(List.of(itemLeido("ALICORC1", "Diversas", "Soles", t2, 2.5)), t2);
    em.flush();

    assertThat(lecturaRepository.count()).isEqualTo(2);
    assertThat(itemRepository.count()).isEqualTo(2);
    assertThat(accionRepository.count()).isEqualTo(1);
  }

  @Test
  @DisplayName("saveData con una lista vacia no crea nada, ni siquiera la lectura")
  void saveDataListaVaciaNoCreaNada() {
    service.saveData(List.of(), LocalDateTime.of(2024, 1, 15, 10, 0));
    em.flush();

    // La Lectura solo se crea a traves de los items, no a partir del parametro fecha.
    assertThat(itemRepository.count()).isZero();
    assertThat(lecturaRepository.count()).isZero();
  }

  @Test
  @DisplayName("CARACTERIZACION: getLastDate devuelve la lectura MAS ANTIGUA")
  void getLastDateDevuelveLaMasAntigua() {
    // Ordena Direction.ASC y toma la primera pagina, asi que pese al nombre no es la ultima.
    // BVL2.process() la usa como referencia para cargar dataAnt (que ademas nunca se lee).
    LocalDateTime pronto = LocalDateTime.of(2024, 1, 15, 9, 45);
    LocalDateTime tarde = LocalDateTime.of(2024, 1, 15, 16, 30);
    service.saveData(List.of(itemLeido("BAP", "Bancos", "Soles", tarde, 1.0)), tarde);
    service.saveData(List.of(itemLeido("BAP", "Bancos", "Soles", pronto, 1.0)), pronto);
    em.flush();

    assertThat(service.getLastDate()).isEqualTo(pronto);
  }

  @Test
  @DisplayName("getLastDate devuelve null con la base vacia")
  void getLastDateSinDatos() {
    // BVL2.process() cuenta con este null y lo sustituye por LocalDateTime.MIN.
    assertThat(service.getLastDate()).isNull();
  }

  @Test
  @DisplayName("getItems(fecha) devuelve los items de esa lectura exacta")
  void getItemsPorFechaExacta() {
    LocalDateTime t1 = LocalDateTime.of(2024, 1, 15, 10, 0);
    LocalDateTime t2 = LocalDateTime.of(2024, 1, 15, 10, 20);
    service.saveData(List.of(
        itemLeido("ALICORC1", "Diversas", "Soles", t1, 1.5),
        itemLeido("BAP", "Bancos", "Soles", t1, -1.0)), t1);
    service.saveData(List.of(itemLeido("ALICORC1", "Diversas", "Soles", t2, 2.5)), t2);
    em.flush();

    assertThat(service.getItems(t1)).hasSize(2);
    assertThat(service.getItems(t2)).hasSize(1);
  }

  @Test
  @DisplayName("getItems(desde, hasta) agrupa todas las lecturas del rango")
  void getItemsPorRango() {
    LocalDateTime t1 = LocalDateTime.of(2024, 1, 15, 10, 0);
    LocalDateTime t2 = LocalDateTime.of(2024, 1, 15, 10, 20);
    service.saveData(List.of(itemLeido("ALICORC1", "Diversas", "Soles", t1, 1.5)), t1);
    service.saveData(List.of(itemLeido("BAP", "Bancos", "Soles", t2, 2.5)), t2);
    em.flush();

    assertThat(service.getItems(t1, t2)).hasSize(2);
    assertThat(service.getItems(t1, t1)).hasSize(1);
  }

  @Test
  @DisplayName("CARACTERIZACION: getHoras lanza DateTimeException siempre")
  void getHorasSiempreFalla() {
    // LocalDateTime.from(fecha.toLocalDate()) no puede construir una hora a partir de un
    // LocalDate: no hay campos de tiempo. El metodo esta roto para cualquier entrada.
    // Hoy no lo llama nadie en produccion.
    LocalDateTime fecha = LocalDateTime.of(2024, 1, 15, 10, 0);
    service.saveData(List.of(itemLeido("BAP", "Bancos", "Soles", fecha, 1.0)), fecha);
    em.flush();

    assertThatThrownBy(() -> service.getHoras(fecha))
        .isInstanceOf(DateTimeException.class);
  }
}
