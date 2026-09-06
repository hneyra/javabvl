package bvl.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * {@code datesEntre} no usa ninguna dependencia inyectada, asi que se prueba sin Spring.
 */
class BvlServiceDatesEntreTest {

  private final BvlService service = new BvlService();

  @Test
  @DisplayName("fecha1 == fecha2 devuelve un unico dia")
  void mismoDiaDevuelveUnElemento() {
    LocalDateTime f = LocalDateTime.of(2024, 1, 15, 10, 30);

    assertThat(service.datesEntre(f, f)).containsExactly(f);
  }

  @Test
  @DisplayName("un rango devuelve un elemento por dia, sin incluir el ultimo")
  void rangoDevuelveUnElementoPorDia() {
    LocalDateTime desde = LocalDateTime.of(2024, 1, 15, 10, 30);
    LocalDateTime hasta = LocalDateTime.of(2024, 1, 18, 10, 30);

    List<LocalDateTime> dates = service.datesEntre(desde, hasta);

    // do-while: agrega y luego comprueba, y corta cuando hasta deja de ser posterior.
    assertThat(dates).containsExactly(
        desde,
        desde.plusDays(1),
        desde.plusDays(2));
  }

  @Test
  @DisplayName("la hora se arrastra a todos los dias del rango")
  void conservaLaHoraDeInicio() {
    LocalDateTime desde = LocalDateTime.of(2024, 1, 15, 9, 41, 7);

    assertThat(service.datesEntre(desde, desde.plusDays(2)))
        .allSatisfy(d -> assertThat(d.toLocalTime()).isEqualTo(desde.toLocalTime()));
  }

  @Test
  @DisplayName("rango invertido devuelve solo la fecha inicial")
  void rangoInvertidoDevuelveSoloLaPrimera() {
    LocalDateTime desde = LocalDateTime.of(2024, 1, 18, 10, 30);
    LocalDateTime hasta = LocalDateTime.of(2024, 1, 15, 10, 30);

    // El do-while agrega antes de comprobar, de ahi que nunca devuelva lista vacia.
    assertThat(service.datesEntre(desde, hasta)).containsExactly(desde);
  }
}
