package bvl.schedule;

import java.time.Instant;
import java.time.LocalDateTime;
import org.springframework.scheduling.Trigger;
import org.springframework.scheduling.TriggerContext;

/**
 * El {@link Trigger} del sondeo: dispara en los instantes que marca {@link HorarioSondeo}, siempre
 * en hora de Lima.
 *
 * <p>Sustituye al {@code CronTrigger} de antes, que solo sabia disparar sobre la rejilla del reloj
 * y en la zona del equipo. Aqui el calculo lo hace {@link HorarioSondeo#siguienteSondeo}, que es
 * una funcion pura y se prueba sin Spring; esta clase solo traduce entre instantes y hora de Lima.
 */
public final class DisparoAnclado implements Trigger {

    private final HorarioSondeo horario;

    public DisparoAnclado(HorarioSondeo horario) {
        this.horario = horario;
    }

    public HorarioSondeo getHorario() {
        return horario;
    }

    @Override
    public Instant nextExecution(TriggerContext contexto) {
        LocalDateTime enLima = LocalDateTime.ofInstant(referencia(contexto), HorarioSondeo.ZONA_MERCADO);
        return horario.siguienteSondeo(enLima).atZone(HorarioSondeo.ZONA_MERCADO).toInstant();
    }

    /**
     * Desde donde contar. La primera vez, desde ahora. Despues, desde el disparo que se acaba de
     * programar; salvo que el sondeo terminara despues del siguiente (la BVL tardo en responder), y
     * entonces desde que termino: se salta el sondeo perdido en vez de lanzarlo atrasado.
     */
    private static Instant referencia(TriggerContext contexto) {
        Instant programado = contexto.lastScheduledExecution();
        if (programado == null) {
            return contexto.getClock().instant();
        }
        Instant completado = contexto.lastCompletion();
        return completado != null && completado.isAfter(programado) ? completado : programado;
    }
}
