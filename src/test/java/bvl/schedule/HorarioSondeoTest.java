package bvl.schedule;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Traduccion de horaInicio/horaFin/intervalo a los instantes en que se sondea.
 *
 * <p>La regla: se sondea a la hora de inicio y luego cada intervalo, <b>contando desde la hora de
 * inicio</b>, hasta la hora de fin incluida; de lunes a viernes. Antes se usaba un cron que
 * disparaba sobre la rejilla del reloj (:00, :05, :10...), asi que con una hora de inicio que no
 * fuera multiplo del intervalo el primer sondeo caia en el siguiente tick de la rejilla y no a la
 * hora configurada.
 *
 * <p>Las fechas son de la semana del martes 2026-09-08; el viernes es el 11 y el lunes el 14.
 */
class HorarioSondeoTest {

  private static final LocalDateTime MARTES_8H = LocalDateTime.of(2026, 9, 8, 8, 0);

  /** Los n primeros sondeos a partir de {@code desde}, como horas del dia. */
  private static List<LocalTime> primeros(HorarioSondeo h, LocalDateTime desde, int n) {
    List<LocalTime> horas = new ArrayList<>();
    LocalDateTime t = desde;
    for (int i = 0; i < n; i++) {
      t = h.siguienteSondeo(t);
      horas.add(t.toLocalTime());
    }
    return horas;
  }

  private static List<LocalTime> horas(String... hhmm) {
    List<LocalTime> horas = new ArrayList<>();
    for (String s : hhmm) {
      horas.add(LocalTime.parse(s));
    }
    return horas;
  }

  // --- Cuando se sondea ---------------------------------------------------------------------

  @Test
  @DisplayName("se sondea a la hora de inicio y cada intervalo contado desde ella")
  void ancladoALaHoraDeInicio() {
    // El caso que fallaba: con el cron salia 10:00, 10:20, 10:40.
    HorarioSondeo h = HorarioSondeo.of("9:45:00", "16:30", "00:20:00");

    assertThat(primeros(h, MARTES_8H, 4)).isEqualTo(horas("09:45", "10:05", "10:25", "10:45"));
  }

  @Test
  @DisplayName("una hora de inicio cualquiera marca la cadencia, no la rejilla del reloj")
  void inicioFueraDeLaRejilla() {
    HorarioSondeo h = HorarioSondeo.of("10:07:00", "16:30", "00:15:00");

    assertThat(primeros(h, MARTES_8H, 3)).isEqualTo(horas("10:07", "10:22", "10:37"));
  }

  @Test
  @DisplayName("los valores de produccion (9:40 cada 20 min) siguen sondeando igual que antes")
  void valoresDeProduccion() {
    HorarioSondeo h = HorarioSondeo.of("9:40:00", "16:30", "00:20:00");

    assertThat(primeros(h, MARTES_8H, 3)).isEqualTo(horas("09:40", "10:00", "10:20"));
  }

  @Test
  @DisplayName("los valores de desarrollo (9:40 cada 5 min) siguen sondeando igual que antes")
  void valoresDeDesarrollo() {
    HorarioSondeo h = HorarioSondeo.of("9:40:00", "16:30", "00:05:00");

    assertThat(primeros(h, MARTES_8H, 3)).isEqualTo(horas("09:40", "09:45", "09:50"));
  }

  @Test
  @DisplayName("el siguiente sondeo es estrictamente posterior: justo en un sondeo, salta al otro")
  void siguienteEsEstrictamentePosterior() {
    HorarioSondeo h = HorarioSondeo.of("9:45:00", "16:30", "00:20:00");

    assertThat(h.siguienteSondeo(LocalDateTime.of(2026, 9, 8, 10, 5)))
        .isEqualTo(LocalDateTime.of(2026, 9, 8, 10, 25));
    // Un disparo que llega unos milisegundos tarde no se repite ni se salta.
    assertThat(h.siguienteSondeo(LocalDateTime.of(2026, 9, 8, 10, 5, 0, 3_000_000)))
        .isEqualTo(LocalDateTime.of(2026, 9, 8, 10, 25));
  }

  @Test
  @DisplayName("al arrancar a media sesion, el siguiente sondeo sigue la cadencia de la hora de inicio")
  void arrancarAMediaSesion() {
    // Pulsar Iniciar a las 11:12 hace una lectura inmediata; el primer sondeo programado es el
    // de las 11:25 (9:45 + 5 x 20 min), no las 11:32.
    HorarioSondeo h = HorarioSondeo.of("9:45:00", "16:30", "00:20:00");

    assertThat(h.siguienteSondeo(LocalDateTime.of(2026, 9, 8, 11, 12)))
        .isEqualTo(LocalDateTime.of(2026, 9, 8, 11, 25));
  }

  @Test
  @DisplayName("la hora de fin se sondea si cae en la cadencia")
  void finIncluido() {
    HorarioSondeo h = HorarioSondeo.of("9:40:00", "16:30", "00:05:00");

    assertThat(h.siguienteSondeo(LocalDateTime.of(2026, 9, 8, 16, 25)))
        .isEqualTo(LocalDateTime.of(2026, 9, 8, 16, 30));
  }

  @Test
  @DisplayName("pasado el ultimo sondeo del dia, el siguiente es la hora de inicio del dia siguiente")
  void trasElUltimoSondeoPasaAlDiaSiguiente() {
    // 9:45 + 20 min: el ultimo que cabe antes de las 16:30 es el de las 16:25.
    HorarioSondeo h = HorarioSondeo.of("9:45:00", "16:30", "00:20:00");

    assertThat(h.siguienteSondeo(LocalDateTime.of(2026, 9, 8, 16, 25)))
        .isEqualTo(LocalDateTime.of(2026, 9, 9, 9, 45));
  }

  @Test
  @DisplayName("del viernes por la tarde salta al lunes: el fin de semana no hay mercado")
  void viernesSaltaAlLunes() {
    HorarioSondeo h = HorarioSondeo.of("9:45:00", "16:30", "00:20:00");

    assertThat(h.siguienteSondeo(LocalDateTime.of(2026, 9, 11, 17, 0)))
        .isEqualTo(LocalDateTime.of(2026, 9, 14, 9, 45));
    assertThat(h.siguienteSondeo(LocalDateTime.of(2026, 9, 12, 11, 0)))
        .as("desde el sabado")
        .isEqualTo(LocalDateTime.of(2026, 9, 14, 9, 45));
  }

  @Test
  @DisplayName("una hora de inicio con segundos se respeta al segundo")
  void inicioConSegundos() {
    HorarioSondeo h = HorarioSondeo.of("9:40:30", "16:30", "00:05:00");

    assertThat(primeros(h, MARTES_8H, 2)).isEqualTo(horas("09:40:30", "09:45:30"));
  }

  @Test
  @DisplayName("las horas se interpretan en hora de Lima, sea cual sea la zona del equipo")
  void zonaDelMercado() {
    assertThat(HorarioSondeo.ZONA_MERCADO).isEqualTo(ZoneId.of("America/Lima"));
  }

  // --- Que intervalo vale -------------------------------------------------------------------

  @Test
  @DisplayName("un intervalo que no divide a 60 ya vale: la regla solo existia por el cron")
  void intervaloNoDivisorDe60() {
    HorarioSondeo h = HorarioSondeo.of("9:40:00", "16:30", "00:07:00");

    assertThat(primeros(h, MARTES_8H, 4)).isEqualTo(horas("09:40", "09:47", "09:54", "10:01"));
  }

  @Test
  @DisplayName("un intervalo de mas de una hora vale si cabe en la franja")
  void intervaloDeMasDeUnaHora() {
    HorarioSondeo h = HorarioSondeo.of("9:40:00", "16:30", "01:30:00");

    assertThat(primeros(h, MARTES_8H, 3)).isEqualTo(horas("09:40", "11:10", "12:40"));
  }

  @Test
  @DisplayName("un intervalo igual a la franja sondea al inicio y al fin")
  void intervaloIgualALaFranja() {
    HorarioSondeo h = HorarioSondeo.of("9:40:00", "10:00", "00:20:00");

    assertThat(primeros(h, MARTES_8H, 2)).isEqualTo(horas("09:40", "10:00"));
  }

  @Test
  @DisplayName("un intervalo mas largo que la franja se rechaza al construir")
  void intervaloMayorQueLaFranja() {
    assertThatThrownBy(() -> HorarioSondeo.of("9:40:00", "10:00", "00:30:00"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("intervalo")
        .hasMessageContaining("00:30:00");
  }

  @Test
  @DisplayName("un intervalo con segundos sueltos se rechaza")
  void intervaloConSegundos() {
    assertThatThrownBy(() -> HorarioSondeo.of("9:40:00", "16:30", "00:05:30"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("minutos enteros");
  }

  @Test
  @DisplayName("un intervalo de cero se rechaza")
  void intervaloCero() {
    assertThatThrownBy(() -> HorarioSondeo.of("9:40:00", "16:30", "00:00:00"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("intervalo");
  }

  // --- Lectura de las propiedades -----------------------------------------------------------

  @Test
  @DisplayName("acepta hora de un digito y segundos opcionales, como estan hoy en properties")
  void parseoTolerante() {
    HorarioSondeo h = HorarioSondeo.of("9:40:00", "16:30", "00:20:00");

    assertThat(h.getInicio()).isEqualTo(LocalTime.of(9, 40));
    assertThat(h.getFin()).isEqualTo(LocalTime.of(16, 30));
    assertThat(h.getIntervaloMinutos()).isEqualTo(20);
  }

  @Test
  @DisplayName("la ventana incluye ambos extremos")
  void ventanaInclusiva() {
    HorarioSondeo h = HorarioSondeo.of("9:40:00", "16:30", "00:20:00");

    assertThat(h.dentroDeVentana(LocalTime.of(9, 40))).isTrue();
    assertThat(h.dentroDeVentana(LocalTime.of(16, 30))).isTrue();
    assertThat(h.dentroDeVentana(LocalTime.of(12, 0))).isTrue();
  }

  @Test
  @DisplayName("la ventana descarta horas anteriores a la apertura o posteriores al cierre")
  void ventanaDescartaFueraDeHorario() {
    HorarioSondeo h = HorarioSondeo.of("9:40:00", "16:30", "00:20:00");

    assertThat(h.dentroDeVentana(LocalTime.of(9, 20))).isFalse();
    assertThat(h.dentroDeVentana(LocalTime.of(16, 40))).isFalse();
    assertThat(h.dentroDeVentana(LocalTime.of(3, 0))).isFalse();
  }

  @Test
  @DisplayName("horaFin anterior o igual a horaInicio se rechaza")
  void ventanaInvalida() {
    assertThatThrownBy(() -> HorarioSondeo.of("16:30:00", "9:40:00", "00:05:00"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("horaFin");
    assertThatThrownBy(() -> HorarioSondeo.of("9:40:00", "9:40:00", "00:05:00"))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  @DisplayName("parseDuracion lee el mismo formato hh:mm:ss que las horas")
  void parseDuracion() {
    assertThat(HorarioSondeo.parseDuracion("00:15:00", "alertaTimeout"))
        .isEqualTo(java.time.Duration.ofMinutes(15));
    assertThat(HorarioSondeo.parseDuracion("01:30:00", "alertaTimeout"))
        .isEqualTo(java.time.Duration.ofMinutes(90));
    assertThat(HorarioSondeo.parseDuracion("00:00:30", "alertaTimeout"))
        .isEqualTo(java.time.Duration.ofSeconds(30));
  }

  @Test
  @DisplayName("parseDuracion nombra la propiedad culpable cuando el valor no vale")
  void parseDuracionInvalida() {
    assertThatThrownBy(() -> HorarioSondeo.parseDuracion("quince minutos", "alertaTimeout"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("alertaTimeout");
  }

  @Test
  @DisplayName("una hora mal escrita se rechaza nombrando la propiedad culpable")
  void horaMalFormada() {
    assertThatThrownBy(() -> HorarioSondeo.of("las nueve", "16:30", "00:05:00"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("horaInicio");
  }
}
