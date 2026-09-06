package bvl.alert;

import static org.assertj.core.api.Assertions.assertThat;

import bvl.domain.Accion;
import bvl.domain.Item;
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

  private static Item item(String nemonico, Double variacion) {
    Accion accion = new Accion();
    accion.setNemonico(nemonico);
    Item item = new Item();
    item.setAccion(accion);
    item.setVariacionPorcentual(variacion);
    return item;
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
}
