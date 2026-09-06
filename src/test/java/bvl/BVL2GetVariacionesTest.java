package bvl;

import static org.assertj.core.api.Assertions.assertThat;

import bvl.domain.Accion;
import bvl.domain.Item;
import java.util.List;
import java.util.Locale;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * Mensaje de alertas que va al TrayIcon y al JOptionPane.
 *
 * <p>Es la logica que el README declara arreglada en la version 3.0.1 ("la variacion de las acciones
 * no se generaba correctamente en el mensaje"), asi que conviene tenerla sujeta.
 *
 * <p>{@code data} lo rellena {@code process()} y es privado; aqui se inyecta directamente para
 * probar el calculo aislado de la red y de la BD.
 */
class BVL2GetVariacionesTest {

  private BVL2 bvl;
  private Locale localeOriginal;

  @BeforeEach
  void setUp() {
    // getVariaciones formatea con DecimalFormat.getInstance(), que depende del locale por
    // defecto de la JVM: en de_DE el separador decimal seria coma. Se fija para que el test
    // no dependa de la maquina.
    localeOriginal = Locale.getDefault();
    Locale.setDefault(Locale.US);
    bvl = new BVL2();
  }

  @AfterEach
  void tearDown() {
    Locale.setDefault(localeOriginal);
  }

  private static Item item(String nemonico, Double variacion) {
    Accion accion = new Accion();
    accion.setNemonico(nemonico);
    Item item = new Item();
    item.setAccion(accion);
    item.setVariacionPorcentual(variacion);
    return item;
  }

  private void conDatos(Item... items) {
    ReflectionTestUtils.setField(bvl, "data", List.of(items));
  }

  @Test
  @DisplayName("sin items que superen el umbral devuelve el mensaje neutro en ambas posiciones")
  void sinVariacionesDevuelveMensajeNeutro() {
    conDatos(item("ALICORC1", 0.5), item("BAP", -1.2));

    assertThat(bvl.getVariaciones(2.0))
        .containsExactly("Sin variaciones.", "Sin variaciones.");
  }

  @Test
  @DisplayName("una subida se marca con '+' y 'subio'")
  void subidaSobreElUmbral() {
    conDatos(item("ALICORC1", 3.5));

    String[] msg = bvl.getVariaciones(2.0);

    assertThat(msg[0]).isEqualTo(" + ALICORC1 subió 3.5%\n");
    assertThat(msg[1])
        .startsWith("<html>")
        .endsWith("</html>")
        .contains("<div style='color:blue'> + ALICORC1 subió 3.5%</div>");
  }

  @Test
  @DisplayName("una bajada se marca con '-' y 'bajo', y en rojo")
  void bajadaSobreElUmbral() {
    conDatos(item("BAP", -4.25));

    String[] msg = bvl.getVariaciones(2.0);

    assertThat(msg[0]).isEqualTo(" - BAP bajó -4.25%\n");
    assertThat(msg[1]).contains("<div style='color:red'> - BAP bajó -4.25%</div>");
  }

  @Test
  @DisplayName("el umbral es inclusivo: una variacion igual al umbral se reporta")
  void umbralInclusivo() {
    conDatos(item("VOLCABC1", 2.0));

    assertThat(bvl.getVariaciones(2.0)[0]).isEqualTo(" + VOLCABC1 subió 2%\n");
  }

  @Test
  @DisplayName("las acciones sin variacion publicada se ignoran, no rompen el mensaje")
  void variacionNulaSeIgnora() {
    conDatos(item("SINDATO", null), item("BAP", 5.0));

    String[] msg = bvl.getVariaciones(1.0);

    assertThat(msg[0]).doesNotContain("SINDATO").isEqualTo(" + BAP subió 5%\n");
  }

  @Test
  @DisplayName("se acumulan varias acciones, una por linea")
  void acumulaVariasAcciones() {
    conDatos(item("ALICORC1", 3.5), item("BAP", -4.0), item("IGNORADA", 0.1));

    String[] msg = bvl.getVariaciones(2.0);

    assertThat(msg[0].split("\n")).containsExactly(
        " + ALICORC1 subió 3.5%",
        " - BAP bajó -4%");
  }

  @Test
  @DisplayName("el formato redondea a dos decimales")
  void redondeaADosDecimales() {
    conDatos(item("BAP", 3.14159));

    assertThat(bvl.getVariaciones(2.0)[0]).isEqualTo(" + BAP subió 3.14%\n");
  }

  @Test
  @DisplayName("una variacion de 0 con umbral 0 se reporta como bajada")
  void ceroConUmbralCeroCuentaComoBajada() {
    // delta > 0 es falso para 0, asi que cae en la rama de bajada.
    conDatos(item("PLANA", 0.0));

    assertThat(bvl.getVariaciones(0.0)[0]).isEqualTo(" - PLANA bajó 0%\n");
  }
}
