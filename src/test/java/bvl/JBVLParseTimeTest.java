package bvl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * {@link JBVL#parseTime(String)} es estatico, asi que no instancia el JFrame.
 *
 * <p>Define el periodo real del bucle de sondeo a partir de la propiedad {@code intervalo}.
 */
class JBVLParseTimeTest {

  @Test
  @DisplayName("hh:mm:ss se convierte a segundos")
  void convierteHoraMinutoSegundo() {
    assertThat(JBVL.parseTime("00:05:00")).isEqualTo(300);
    assertThat(JBVL.parseTime("00:20:00")).isEqualTo(1200);
    assertThat(JBVL.parseTime("2:15:23")).isEqualTo(2 * 3600 + 15 * 60 + 23);
    assertThat(JBVL.parseTime("00:00:00")).isZero();
  }

  @Test
  @DisplayName("con dos tokens el primero sigue valiendo horas, no minutos")
  void dosTokensSeInterpretanComoHorasYMinutos() {
    // El exponente arranca en 2 pase lo que pase, asi que "16:30" son 16h30m, no 16m30s.
    assertThat(JBVL.parseTime("16:30")).isEqualTo(16 * 3600 + 30 * 60);
  }

  @Test
  @DisplayName("un unico token se interpreta como horas")
  void unSoloTokenSonHoras() {
    assertThat(JBVL.parseTime("1")).isEqualTo(3600);
  }

  @Test
  @DisplayName("un intervalo no numerico revienta el hilo de sondeo")
  void intervaloNoNumericoLanzaNumberFormatException() {
    // El campo de la UI es texto libre y parseTime se llama sin validar.
    assertThatThrownBy(() -> JBVL.parseTime("cada rato"))
        .isInstanceOf(NumberFormatException.class);
  }
}
