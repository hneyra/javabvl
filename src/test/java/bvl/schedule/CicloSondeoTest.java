package bvl.schedule;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import bvl.domain.Accion;
import bvl.domain.Item;
import bvl.market.LectorBvl;
import bvl.service.ExportService;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/**
 * El recorrido de una lectura y la memoria de la anterior.
 *
 * <p>Esa memoria es lo unico que la aplicacion retiene entre sondeos, y es lo que permite medir el
 * movimiento intradia sin guardar nada en disco.
 */
class CicloSondeoTest {

  private static final LocalDateTime T1 = LocalDateTime.of(2024, 1, 15, 10, 30);
  private static final LocalDateTime T2 = LocalDateTime.of(2024, 1, 15, 10, 50);

  private LectorBvl lector;
  private ExportService exportacion;
  private CicloSondeo ciclo;

  @BeforeEach
  void setUp() {
    lector = mock(LectorBvl.class);
    exportacion = mock(ExportService.class);
    ciclo = new CicloSondeo(lector, exportacion);
  }

  private static Item item(String nemonico) {
    Accion accion = new Accion();
    accion.setNemonico(nemonico);
    Item item = new Item();
    item.setAccion(accion);
    return item;
  }

  private void laBvlDevuelve(LocalDateTime fecha, Item... items) {
    when(lector.readData()).thenReturn(List.of(items));
    when(lector.getFecha()).thenReturn(fecha);
  }

  @Test
  @DisplayName("el primer sondeo tras arrancar no tiene con que comparar")
  void primerSondeoSinComparacion() {
    laBvlDevuelve(T1, item("ALICORC1"));

    ResultadoSondeo resultado = ciclo.process();

    assertThat(resultado.actual().fecha()).isEqualTo(T1);
    assertThat(resultado.previa()).isNull();
    assertThat(resultado.hayConQueComparar()).isFalse();
  }

  @Test
  @DisplayName("el segundo sondeo llega con la lectura del primero para comparar")
  void segundoSondeoTraeLaLecturaAnterior() {
    laBvlDevuelve(T1, item("ALICORC1"));
    ciclo.process();
    laBvlDevuelve(T2, item("ALICORC1"), item("BAP"));

    ResultadoSondeo resultado = ciclo.process();

    assertThat(resultado.actual().fecha()).isEqualTo(T2);
    assertThat(resultado.actual().items()).hasSize(2);
    assertThat(resultado.previa().fecha()).isEqualTo(T1);
    assertThat(resultado.previa().items()).hasSize(1);
    assertThat(resultado.hayConQueComparar()).isTrue();
  }

  @Test
  @DisplayName("solo se retiene la lectura inmediatamente anterior, no toda la sesion")
  void soloSeRetieneLaAnterior() {
    // Encadenar resultados iria acumulando la sesion entera en memoria.
    laBvlDevuelve(T1, item("A"));
    ciclo.process();
    laBvlDevuelve(T2, item("A"));
    ciclo.process();
    laBvlDevuelve(T2.plusMinutes(20), item("A"));

    ResultadoSondeo tercero = ciclo.process();

    assertThat(tercero.previa().fecha()).isEqualTo(T2);
  }

  @Test
  @DisplayName("si la BVL republica el mismo instante no es un movimiento, es la misma lectura")
  void mismaFechaNoEsComparable() {
    laBvlDevuelve(T1, item("ALICORC1"));
    ciclo.process();
    laBvlDevuelve(T1, item("ALICORC1"));

    ResultadoSondeo resultado = ciclo.process();

    assertThat(resultado.previa()).isNotNull();
    assertThat(resultado.hayConQueComparar()).isFalse();
  }

  @Test
  @DisplayName("cada ciclo exporta la lectura que acaba de hacer")
  void cadaCicloExporta() {
    laBvlDevuelve(T1, item("ALICORC1"));
    ciclo.process();
    laBvlDevuelve(T2, item("ALICORC1"));
    ciclo.process();

    ArgumentCaptor<Lectura> captor = ArgumentCaptor.forClass(Lectura.class);
    verify(exportacion, times(2)).exportar(captor.capture());
    assertThat(captor.getAllValues()).extracting(Lectura::fecha).containsExactly(T1, T2);
  }
}
