package bvl.alert;

import static org.assertj.core.api.Assertions.assertThat;

import bvl.domain.Accion;
import bvl.domain.Item;
import bvl.schedule.Lectura;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Criterio de seleccion de las acciones que disparan alerta.
 *
 * <p>Antes esto vivia enredado con la construccion del mensaje HTML y no habia forma de probarlo
 * por separado.
 */
class DetectorVariacionesTest {

  private final DetectorVariaciones detector = new DetectorVariaciones();

  private static final LocalDateTime T1 = LocalDateTime.of(2024, 1, 15, 10, 30);
  private static final LocalDateTime T2 = LocalDateTime.of(2024, 1, 15, 10, 50);

  private static Item item(String nemonico, Double variacion) {
    Accion accion = new Accion();
    accion.setNemonico(nemonico);
    Item item = new Item();
    item.setAccion(accion);
    item.setVariacionPorcentual(variacion);
    return item;
  }

  /** Item con precio, que es lo que mira la comparacion entre lecturas. */
  private static Item cotiza(String nemonico, Double ultima) {
    Item item = item(nemonico, null);
    item.setCotizacionUltima(ultima);
    return item;
  }

  private static Lectura lectura(LocalDateTime fecha, Item... items) {
    return new Lectura(List.of(items), fecha);
  }

  @Test
  @DisplayName("descarta lo que no llega al umbral, en subida y en bajada")
  void descartaLoQueNoLlegaAlUmbral() {
    List<Variacion> variaciones =
        detector.detectar(List.of(item("ALICORC1", 0.5), item("BAP", -1.2)), 2.0);

    assertThat(variaciones).isEmpty();
  }

  @Test
  @DisplayName("el umbral es inclusivo: una variacion igual al umbral se reporta")
  void umbralInclusivo() {
    assertThat(detector.detectar(List.of(item("VOLCABC1", 2.0)), 2.0))
        .containsExactly(new Variacion("VOLCABC1", 2.0));
  }

  @Test
  @DisplayName("el umbral se aplica en valor absoluto: una caida fuerte tambien alerta")
  void umbralEnValorAbsoluto() {
    assertThat(detector.detectar(List.of(item("BAP", -4.25)), 2.0))
        .containsExactly(new Variacion("BAP", -4.25));
  }

  @Test
  @DisplayName("las acciones sin variacion publicada se ignoran, no rompen la deteccion")
  void variacionNulaSeIgnora() {
    // La BVL deja a null los instrumentos que no se han negociado en el dia.
    List<Variacion> variaciones =
        detector.detectar(List.of(item("SINDATO", null), item("BAP", 5.0)), 1.0);

    assertThat(variaciones).extracting(Variacion::nemonico).containsExactly("BAP");
  }

  @Test
  @DisplayName("conserva el orden en que la BVL publico las acciones")
  void conservaElOrdenDePublicacion() {
    List<Variacion> variaciones = detector.detectar(
        List.of(item("ALICORC1", 3.5), item("BAP", -4.0), item("IGNORADA", 0.1)), 2.0);

    assertThat(variaciones).extracting(Variacion::nemonico)
        .containsExactly("ALICORC1", "BAP");
  }

  @Test
  @DisplayName("una lista vacia no produce variaciones")
  void listaVacia() {
    assertThat(detector.detectar(List.of(), 2.0)).isEmpty();
  }

  @Test
  @DisplayName("una variacion de 0 con umbral 0 cuenta como bajada")
  void ceroConUmbralCeroCuentaComoBajada() {
    // La condicion de subida es > 0, no >= 0, asi que el 0 cae en la rama de bajada.
    List<Variacion> variaciones = detector.detectar(List.of(item("PLANA", 0.0)), 0.0);

    assertThat(variaciones).hasSize(1);
    assertThat(variaciones.get(0).esSubida()).isFalse();
  }

  // --- Movimiento desde el sondeo anterior --------------------------------------------------

  @Test
  @DisplayName("calcula el porcentaje entre la cotizacion anterior y la actual")
  void movimientoEntreLecturas() {
    Lectura previa = lectura(T1, cotiza("ALICORC1", 10.0));
    Lectura actual = lectura(T2, cotiza("ALICORC1", 11.0));

    assertThat(detector.detectarDesde(previa, actual, 5.0))
        .containsExactly(new Variacion("ALICORC1", 10.0));
  }

  @Test
  @DisplayName("una caida entre lecturas sale con signo negativo")
  void caidaEntreLecturas() {
    Lectura previa = lectura(T1, cotiza("BAP", 10.0));
    Lectura actual = lectura(T2, cotiza("BAP", 9.0));

    assertThat(detector.detectarDesde(previa, actual, 5.0))
        .singleElement()
        .satisfies(v -> {
          assertThat(v.porcentaje()).isCloseTo(-10.0, org.assertj.core.data.Offset.offset(1e-9));
          assertThat(v.esSubida()).isFalse();
        });
  }

  @Test
  @DisplayName("por debajo del umbral no se reporta")
  void movimientoPequenoNoAlerta() {
    Lectura previa = lectura(T1, cotiza("ALICORC1", 10.0));
    Lectura actual = lectura(T2, cotiza("ALICORC1", 10.1));

    assertThat(detector.detectarDesde(previa, actual, 5.0)).isEmpty();
  }

  @Test
  @DisplayName("una accion que no estaba en la lectura anterior no se ha movido: se ignora")
  void accionNuevaSeIgnora() {
    // Aparece a media sesion; no hay contra que medirla.
    Lectura previa = lectura(T1, cotiza("ALICORC1", 10.0));
    Lectura actual = lectura(T2, cotiza("ALICORC1", 10.0), cotiza("NUEVA", 50.0));

    assertThat(detector.detectarDesde(previa, actual, 0.0))
        .extracting(Variacion::nemonico).containsExactly("ALICORC1");
  }

  @Test
  @DisplayName("sin precio en alguna de las dos lecturas no hay comparacion posible")
  void sinPrecioNoHayComparacion() {
    // La BVL deja de publicar precio en cuanto un instrumento no se negocia.
    Lectura previa = lectura(T1, cotiza("SINPRECIO", null), cotiza("SEAPAGA", 10.0));
    Lectura actual = lectura(T2, cotiza("SINPRECIO", 10.0), cotiza("SEAPAGA", null));

    assertThat(detector.detectarDesde(previa, actual, 0.0)).isEmpty();
  }

  @Test
  @DisplayName("un precio anterior de cero se descarta en vez de producir un infinito")
  void precioAnteriorCeroSeDescarta() {
    Lectura previa = lectura(T1, cotiza("RARA", 0.0));
    Lectura actual = lectura(T2, cotiza("RARA", 5.0));

    assertThat(detector.detectarDesde(previa, actual, 0.0)).isEmpty();
  }

  @Test
  @DisplayName("un precio que no se mueve no alerta salvo con umbral cero")
  void precioQuietoNoAlerta() {
    Lectura previa = lectura(T1, cotiza("PLANA", 10.0));
    Lectura actual = lectura(T2, cotiza("PLANA", 10.0));

    assertThat(detector.detectarDesde(previa, actual, 0.5)).isEmpty();
    assertThat(detector.detectarDesde(previa, actual, 0.0))
        .containsExactly(new Variacion("PLANA", 0.0));
  }
}
