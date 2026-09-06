package bvl.alert;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Redaccion del aviso que va al globo de la bandeja y al dialogo.
 *
 * <p>Es la logica que el README declara arreglada en la version 3.0.1 ("la variacion de las
 * acciones no se generaba correctamente en el mensaje"), asi que conviene tenerla sujeta al
 * caracter.
 */
class AlertaFormatterTest {

  private final AlertaFormatter formatter = new AlertaFormatter();
  private Locale localeOriginal;

  @BeforeEach
  void fijarLocale() {
    // TRAMPA CONOCIDA: el formato usa DecimalFormat.getInstance(), que depende del locale por
    // defecto de la JVM; en de_DE el separador decimal seria coma. Se fija para que el test no
    // dependa de la maquina.
    localeOriginal = Locale.getDefault();
    Locale.setDefault(Locale.US);
  }

  @AfterEach
  void restaurarLocale() {
    Locale.setDefault(localeOriginal);
  }

  @Test
  @DisplayName("sin variaciones devuelve el mensaje neutro en los dos formatos")
  void sinVariacionesDevuelveMensajeNeutro() {
    assertThat(formatter.texto(List.of())).isEqualTo("Sin variaciones.");
    assertThat(formatter.html(List.of())).isEqualTo("Sin variaciones.");
  }

  @Test
  @DisplayName("una subida se marca con '+' y 'subio', y en azul")
  void subida() {
    List<Variacion> variaciones = List.of(new Variacion("ALICORC1", 3.5));

    assertThat(formatter.texto(variaciones)).isEqualTo(" + ALICORC1 subió 3.5%\n");
    assertThat(formatter.html(variaciones))
        .startsWith("<html>")
        .endsWith("</html>")
        .contains("<div style='color:blue'> + ALICORC1 subió 3.5%</div>");
  }

  @Test
  @DisplayName("una bajada se marca con '-' y 'bajo', y en rojo")
  void bajada() {
    List<Variacion> variaciones = List.of(new Variacion("BAP", -4.25));

    assertThat(formatter.texto(variaciones)).isEqualTo(" - BAP bajó -4.25%\n");
    assertThat(formatter.html(variaciones))
        .contains("<div style='color:red'> - BAP bajó -4.25%</div>");
  }

  @Test
  @DisplayName("se acumulan varias acciones, una por linea")
  void acumulaVariasAcciones() {
    String texto = formatter.texto(
        List.of(new Variacion("ALICORC1", 3.5), new Variacion("BAP", -4.0)));

    assertThat(texto.split("\n")).containsExactly(
        " + ALICORC1 subió 3.5%",
        " - BAP bajó -4%");
  }

  @Test
  @DisplayName("el formato redondea a dos decimales")
  void redondeaADosDecimales() {
    assertThat(formatter.texto(List.of(new Variacion("BAP", 3.14159))))
        .isEqualTo(" + BAP subió 3.14%\n");
  }

  @Test
  @DisplayName("una variacion de cero se redacta como bajada")
  void ceroSeRedactaComoBajada() {
    assertThat(formatter.texto(List.of(new Variacion("PLANA", 0.0))))
        .isEqualTo(" - PLANA bajó 0%\n");
  }

  @Test
  @DisplayName("el HTML lleva el encabezado y una fila por accion")
  void htmlLlevaEncabezadoYUnaFilaPorAccion() {
    String html = formatter.html(
        List.of(new Variacion("ALICORC1", 3.5), new Variacion("BAP", -4.0)));

    assertThat(html).contains("<b>Las siguientes empresas variaron:</b><br /><br />");
    assertThat(html).contains("<div style='color:blue'>").contains("<div style='color:red'>");
  }

  // --- Con bloque de movimiento intradia ----------------------------------------------------

  private static final LocalDateTime T_ANTERIOR = LocalDateTime.of(2024, 1, 15, 10, 30);

  @Test
  @DisplayName("el bloque intradia se añade debajo, separado y con la hora de la lectura previa")
  void bloqueIntradiaEnTexto() {
    String texto = formatter.texto(
        List.of(new Variacion("ALICORC1", 3.5)),
        List.of(new Variacion("BAP", -2.5)),
        T_ANTERIOR);

    assertThat(texto).isEqualTo(" + ALICORC1 subió 3.5%\n\nDesde las 10:30:\n - BAP bajó -2.5%\n");
  }

  @Test
  @DisplayName("en HTML el bloque intradia lleva su propio encabezado")
  void bloqueIntradiaEnHtml() {
    String html = formatter.html(
        List.of(new Variacion("ALICORC1", 3.5)),
        List.of(new Variacion("BAP", -2.5)),
        T_ANTERIOR);

    assertThat(html)
        .startsWith("<html>")
        .endsWith("</html>")
        .contains("<b>Las siguientes empresas variaron:</b>")
        .contains("<div style='color:blue'> + ALICORC1 subió 3.5%</div>")
        .contains("<b>Desde la lectura de las 10:30:</b><br /><br />")
        .contains("<div style='color:red'> - BAP bajó -2.5%</div>");
  }

  @Test
  @DisplayName("sin movimiento intradia el mensaje sale identico al de siempre")
  void sinIntradiaElMensajeNoCambia() {
    // Es la garantia de que añadir la comparacion no altera lo que el usuario lleva viendo.
    List<Variacion> variaciones = List.of(new Variacion("ALICORC1", 3.5));

    assertThat(formatter.texto(variaciones, List.of(), T_ANTERIOR))
        .isEqualTo(formatter.texto(variaciones));
    assertThat(formatter.html(variaciones, List.of(), T_ANTERIOR))
        .isEqualTo(formatter.html(variaciones));
  }

  @Test
  @DisplayName("solo movimiento intradia: se muestra ese bloque, sin el encabezado del otro")
  void soloIntradia() {
    List<Variacion> intradia = List.of(new Variacion("BAP", -2.5));

    assertThat(formatter.texto(List.of(), intradia, T_ANTERIOR))
        .isEqualTo("Desde las 10:30:\n - BAP bajó -2.5%\n");
    assertThat(formatter.html(List.of(), intradia, T_ANTERIOR))
        .doesNotContain("Las siguientes empresas variaron")
        .contains("<b>Desde la lectura de las 10:30:</b>");
  }

  @Test
  @DisplayName("los dos bloques vacios siguen dando el mensaje neutro")
  void ambosVaciosDanMensajeNeutro() {
    assertThat(formatter.texto(List.of(), List.of(), T_ANTERIOR)).isEqualTo("Sin variaciones.");
    assertThat(formatter.html(List.of(), List.of(), T_ANTERIOR)).isEqualTo("Sin variaciones.");
  }
}
