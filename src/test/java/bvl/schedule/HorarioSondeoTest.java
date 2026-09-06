package bvl.schedule;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Traduccion de las propiedades horaInicio/horaFin/intervalo a expresion cron y ventana horaria.
 *
 * <p>Los valores de los dos ficheros de properties del repo son los casos de referencia:
 * desarrollo usa intervalo 00:05:00 y produccion 00:20:00, ambos con horaInicio 9:40:00 y
 * horaFin 16:30.
 */
class HorarioSondeoTest {

  @Test
  @DisplayName("el intervalo de produccion (20 min) genera la rejilla :00 :20 :40")
  void cronDeProduccion() {
    HorarioSondeo h = HorarioSondeo.of("9:40:00", "16:30", "00:20:00");

    assertThat(h.toCron()).isEqualTo("0 0/20 9-16 * * MON-FRI");
  }

  @Test
  @DisplayName("el intervalo de desarrollo (5 min) genera la rejilla de 5 en 5")
  void cronDeDesarrollo() {
    HorarioSondeo h = HorarioSondeo.of("9:40:00", "16:30", "00:05:00");

    assertThat(h.toCron()).isEqualTo("0 0/5 9-16 * * MON-FRI");
  }

  @Test
  @DisplayName("un intervalo de 60 minutos dispara solo en punto")
  void cronCadaHora() {
    assertThat(HorarioSondeo.of("9:00:00", "16:00:00", "01:00:00").toCron())
        .isEqualTo("0 0 9-16 * * MON-FRI");
  }

  @Test
  @DisplayName("si inicio y fin caen en la misma hora, el campo de horas no lleva rango")
  void cronDeUnaSolaHora() {
    assertThat(HorarioSondeo.of("9:05:00", "9:55:00", "00:05:00").toCron())
        .isEqualTo("0 0/5 9 * * MON-FRI");
  }

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
  @DisplayName("la ventana descarta los disparos de la rejilla anteriores o posteriores")
  void ventanaDescartaFueraDeHorario() {
    // El cron dispara a las 9:00 y 9:20 porque la hora 9 entra entera en el rango,
    // pero la sesion no ha abierto: la guarda los descarta.
    HorarioSondeo h = HorarioSondeo.of("9:40:00", "16:30", "00:20:00");

    assertThat(h.dentroDeVentana(LocalTime.of(9, 20))).isFalse();
    assertThat(h.dentroDeVentana(LocalTime.of(16, 40))).isFalse();
    assertThat(h.dentroDeVentana(LocalTime.of(3, 0))).isFalse();
  }

  @Test
  @DisplayName("un intervalo que no divide a 60 se rechaza al construir")
  void intervaloNoDivisorDe60() {
    assertThatThrownBy(() -> HorarioSondeo.of("9:40:00", "16:30", "00:07:00"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("00:07:00")
        .hasMessageContaining("divisor");
  }

  @Test
  @DisplayName("un intervalo con segundos sueltos se rechaza")
  void intervaloConSegundos() {
    assertThatThrownBy(() -> HorarioSondeo.of("9:40:00", "16:30", "00:05:30"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("minutos enteros");
  }

  @Test
  @DisplayName("un intervalo de cero o mayor que una hora se rechaza")
  void intervaloFueraDeRango() {
    assertThatThrownBy(() -> HorarioSondeo.of("9:40:00", "16:30", "00:00:00"))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> HorarioSondeo.of("9:40:00", "16:30", "02:00:00"))
        .isInstanceOf(IllegalArgumentException.class);
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
