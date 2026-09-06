package bvl.repository;

import static org.assertj.core.api.Assertions.assertThat;

import bvl.domain.Accion;
import bvl.domain.Item;
import bvl.domain.Lectura;
import bvl.domain.Moneda;
import bvl.domain.Sector;
import bvl.support.TestJpaConfig;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.test.context.ContextConfiguration;

/**
 * Metodos derivados de los repositorios contra H2. Fijan el contrato del que depende
 * {@code BvlService} para agrupar y exportar.
 */
@DataJpaTest
@ContextConfiguration(classes = TestJpaConfig.class)
class RepositoriesIntegrationTest {

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

  private Lectura lectura(LocalDateTime fecha) {
    Lectura l = new Lectura();
    l.setFecha(fecha);
    return em.persist(l);
  }

  private Accion accion(String nemonico, String sectorNombre) {
    Sector s = new Sector();
    s.setNombre(sectorNombre);
    em.persist(s);
    Accion a = new Accion();
    a.setNemonico(nemonico);
    a.setEmpresa("Empresa " + nemonico);
    a.setSector(s);
    return em.persist(a);
  }

  /** Moneda compartida: el nombre lleva indice unico, no vale crear una por item. */
  private Moneda soles;

  private Moneda soles() {
    if (soles == null) {
      soles = new Moneda();
      soles.setNombre("Soles");
      em.persist(soles);
    }
    return soles;
  }

  private Item item(Lectura l, Accion a) {
    Moneda m = soles();
    Item i = new Item();
    i.setLectura(l);
    i.setAccion(a);
    i.setMoneda(m);
    i.setSegmento("");
    i.setFechaLectura(l.getFecha());
    return em.persist(i);
  }

  @Test
  @DisplayName("findByFecha localiza la lectura por su instante exacto")
  void findByFecha() {
    LocalDateTime f = LocalDateTime.of(2024, 1, 15, 10, 30, 0);
    lectura(f);
    em.flush();

    assertThat(lecturaRepository.findByFecha(f)).isNotNull();
    assertThat(lecturaRepository.findByFecha(f.plusSeconds(1))).isNull();
  }

  @Test
  @DisplayName("findByFechaBetween incluye ambos extremos")
  void findByFechaBetweenEsInclusivo() {
    LocalDateTime a = LocalDateTime.of(2024, 1, 15, 9, 45);
    LocalDateTime b = LocalDateTime.of(2024, 1, 15, 12, 0);
    LocalDateTime c = LocalDateTime.of(2024, 1, 15, 16, 30);
    lectura(a);
    lectura(b);
    lectura(c);
    em.flush();

    // BvlService.getHoras y getItems dependen de que los bordes entren.
    assertThat(lecturaRepository.findByFechaBetween(a, c))
        .extracting(Lectura::getFecha)
        .containsExactlyInAnyOrder(a, b, c);
    assertThat(lecturaRepository.findByFechaBetween(b, c))
        .extracting(Lectura::getFecha)
        .containsExactlyInAnyOrder(b, c);
  }

  @Test
  @DisplayName("findByFechaBetween con un rango sin lecturas devuelve vacio")
  void findByFechaBetweenSinResultados() {
    lectura(LocalDateTime.of(2024, 1, 15, 10, 0));
    em.flush();

    assertThat(lecturaRepository.findByFechaBetween(
        LocalDateTime.of(2024, 2, 1, 0, 0),
        LocalDateTime.of(2024, 2, 28, 0, 0))).isEmpty();
  }

  @Test
  @DisplayName("findByLectura e findByLecturaIn agrupan los items de cada lectura")
  void findByLecturaYLecturaIn() {
    Lectura l1 = lectura(LocalDateTime.of(2024, 1, 15, 10, 0));
    Lectura l2 = lectura(LocalDateTime.of(2024, 1, 15, 10, 20));
    Accion alicor = accion("ALICORC1", "Diversas");
    Accion bap = accion("BAP", "Bancos");
    item(l1, alicor);
    item(l1, bap);
    item(l2, alicor);
    em.flush();

    assertThat(itemRepository.findByLectura(l1)).hasSize(2);
    assertThat(itemRepository.findByLectura(l2)).hasSize(1);
    assertThat(itemRepository.findByLecturaIn(List.of(l1, l2))).hasSize(3);
    assertThat(itemRepository.findByLecturaIn(List.of())).isEmpty();
  }

  @Test
  @DisplayName("las busquedas por clave natural devuelven null si no existe")
  void busquedasPorClaveNatural() {
    accion("ALICORC1", "Diversas");
    Moneda m = new Moneda();
    m.setNombre("Soles");
    em.persist(m);
    em.flush();

    // Son las que usan los saveIfNotExist* de BvlService para deduplicar.
    assertThat(accionRepository.findByNemonico("ALICORC1")).isNotNull();
    assertThat(accionRepository.findByNemonico("NOEXISTE")).isNull();
    assertThat(monedaRepository.findByNombre("Soles")).isNotNull();
    assertThat(sectorRepository.findByNombre("Diversas")).isNotNull();
    assertThat(sectorRepository.findByNombre("NOEXISTE")).isNull();
  }

  @Test
  @DisplayName("findAll paginado ordena por fecha ascendente")
  void findAllPaginadoOrdenaAscendente() {
    LocalDateTime tarde = LocalDateTime.of(2024, 1, 15, 16, 0);
    LocalDateTime pronto = LocalDateTime.of(2024, 1, 15, 9, 45);
    lectura(tarde);
    lectura(pronto);
    em.flush();

    Page<Lectura> page = lecturaRepository.findAll(PageRequest.of(0, 1, Direction.ASC, "fecha"));

    // Esta consulta es la que usa BvlService.getLastDate: primera pagina ASC = la mas antigua.
    assertThat(page.getContent()).hasSize(1);
    assertThat(page.getContent().get(0).getFecha()).isEqualTo(pronto);
    assertThat(page.getTotalElements()).isEqualTo(2);
  }

  @Test
  @DisplayName("Sector.setNombre convierte null en '---'")
  void sectorNormalizaNombreNulo() {
    // La BVL manda sectorDescription vacio en algunos instrumentos y la columna es NOT NULL.
    Sector s = new Sector();
    s.setNombre(null);
    em.persist(s);
    em.flush();

    assertThat(sectorRepository.findByNombre("---")).isNotNull();
  }
}
