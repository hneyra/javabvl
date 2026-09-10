package bvl.schedule;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.scheduling.support.SimpleTriggerContext;

/**
 * El disparador que usa el planificador: traduce {@link HorarioSondeo#siguienteSondeo} a los
 * instantes que pide Spring, siempre en hora de Lima.
 */
class DisparoAncladoTest {

  private static final ZoneId LIMA = ZoneId.of("America/Lima");
  private static final ZoneId MADRID = ZoneId.of("Europe/Madrid");

  private final HorarioSondeo horario = HorarioSondeo.of("9:45:00", "16:30", "00:20:00");
  private final DisparoAnclado disparo = new DisparoAnclado(horario);

  private static Instant lima(int hora, int minuto) {
    return LocalDateTime.of(2026, 9, 8, hora, minuto).atZone(LIMA).toInstant();
  }

  @Test
  @DisplayName("el primer disparo es el siguiente de la cadencia a partir de ahora")
  void primerDisparo() {
    SimpleTriggerContext contexto = new SimpleTriggerContext(Clock.fixed(lima(10, 12), LIMA));

    assertThat(disparo.nextExecution(contexto)).isEqualTo(lima(10, 25));
  }

  @Test
  @DisplayName("la hora de Lima manda aunque el equipo este en otra zona")
  void independienteDeLaZonaDelEquipo() {
    // Las 10:12 en Lima son las 17:12 en Madrid. Con la zona del equipo, 9:45 significaria las
    // 9:45 de Madrid y no se sondearia en todo el horario de mercado.
    SimpleTriggerContext contexto = new SimpleTriggerContext(Clock.fixed(lima(10, 12), MADRID));

    assertThat(disparo.nextExecution(contexto)).isEqualTo(lima(10, 25));
  }

  @Test
  @DisplayName("tras un sondeo, el siguiente sale de la cadencia, no del momento en que acabo")
  void siguienteTrasUnSondeo() {
    SimpleTriggerContext contexto = new SimpleTriggerContext(Clock.fixed(lima(10, 25), LIMA));
    // Programado a las 10:25, arranco con 4 ms de retraso y tardo 3 s en leer y exportar.
    contexto.update(lima(10, 25), lima(10, 25).plusMillis(4), lima(10, 25).plusSeconds(3));

    assertThat(disparo.nextExecution(contexto)).isEqualTo(lima(10, 45));
  }

  @Test
  @DisplayName("si un sondeo se alarga mas que el intervalo, se salta el que se ha perdido")
  void sondeoLargoSaltaElPerdido() {
    // La BVL tardo en responder: el de las 10:25 acabo a las 10:47, despues del de las 10:45.
    SimpleTriggerContext contexto = new SimpleTriggerContext(Clock.fixed(lima(10, 47), LIMA));
    contexto.update(lima(10, 25), lima(10, 25), lima(10, 47));

    assertThat(disparo.nextExecution(contexto)).isEqualTo(lima(11, 5));
  }

  @Test
  @DisplayName("expone el horario con el que se programo")
  void exponeElHorario() {
    assertThat(disparo.getHorario()).isSameAs(horario);
  }
}
