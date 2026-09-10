package bvl.schedule;

import bvl.config.BvlProperties;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalTime;
import java.util.concurrent.ScheduledFuture;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Service;

/**
 * Programa el sondeo de la BVL: a la hora de inicio y cada intervalo contado desde ella, hasta la
 * hora de fin, de lunes a viernes y en hora de Lima. Los instantes los calcula
 * {@link HorarioSondeo}; {@link DisparoAnclado} los entrega al {@link TaskScheduler}.
 *
 * <p>Ademas hay una guarda de ventana en cada disparo. El disparador ya no programa nada fuera de
 * horario, pero si el equipo se suspende a media sesion, el disparo pendiente se ejecuta al
 * despertar, que puede ser de noche: la guarda lo descarta.
 *
 * <p>La aplicacion arranca en reposo: hasta que alguien pulsa Iniciar no se programa nada.
 */
@Service
public class BvlScheduler {

    /** Evita comprobar null en cada disparo: sin ventana registrada, los avisos se tiran. */
    private static final SondeoListener SIN_LISTENER = new SondeoListener() {
        @Override
        public void onSondeoCompletado(ResultadoSondeo resultado) {
        }

        @Override
        public void onSondeoFallido(Throwable error) {
        }
    };

    private static final Logger logger = LoggerFactory.getLogger(BvlScheduler.class);

    /**
     * Margen de la guarda de ventana. Un disparo programado a las 16:30:00 se ejecuta siempre algo
     * despues; sin margen, 16:30:00.005 quedaba fuera de la ventana y el sondeo de la hora de fin no
     * ocurria nunca.
     */
    private static final Duration TOLERANCIA_RETRASO = Duration.ofSeconds(30);

    private final CicloSondeo ciclo;
    private final TaskScheduler taskScheduler;
    private final Clock clock;

    private volatile HorarioSondeo horario;
    private volatile SondeoListener listener = SIN_LISTENER;
    private ScheduledFuture<?> tarea;

    // @Autowired explicito: hay dos constructores y sin la marca Spring busca el vacio y falla.
    @Autowired
    public BvlScheduler(CicloSondeo ciclo, TaskScheduler taskScheduler, BvlProperties properties) {
        // El horario ya viene validado desde BvlProperties: una configuracion mala impide arrancar
        // la aplicacion en vez de fallar a mitad de sesion.
        this(ciclo, taskScheduler, properties.getHorario(), Clock.system(HorarioSondeo.ZONA_MERCADO));
    }

    BvlScheduler(CicloSondeo ciclo, TaskScheduler taskScheduler, HorarioSondeo horario,
                 Clock clock) {
        this.ciclo = ciclo;
        this.taskScheduler = taskScheduler;
        this.horario = horario;
        this.clock = clock;
    }

    public void setListener(SondeoListener listener) {
        this.listener = listener == null ? SIN_LISTENER : listener;
    }

    public HorarioSondeo getHorario() {
        return horario;
    }

    public synchronized void iniciar() {
        if (isActivo()) {
            return;
        }
        tarea = programar();
        // Lectura inmediata: quien pulsa Iniciar quiere los ultimos datos ya, sin esperar al
        // primer disparo programado. Va al hilo del planificador, nunca al EDT: es red y XLS.
        // El pool es de un solo hilo, asi que no puede solaparse con los disparos programados.
        taskScheduler.schedule(this::sondearAhora, Instant.now(clock));
    }

    /**
     * Cambia el horario sin reiniciar la aplicacion. Si hay sondeo en marcha, se cancela la tarea
     * y se programa otra con el horario nuevo; si esta en reposo, el horario queda listo para el
     * proximo {@link #iniciar()}.
     */
    public synchronized void reprogramar(HorarioSondeo nuevo) {
        this.horario = nuevo;
        if (isActivo()) {
            tarea.cancel(false);
            tarea = programar();
        }
    }

    private ScheduledFuture<?> programar() {
        logger.info("Programando sondeo: {}", horario);
        return taskScheduler.schedule(this::ejecutarSondeo, new DisparoAnclado(horario));
    }

    public synchronized void detener() {
        if (tarea != null) {
            tarea.cancel(false);
            tarea = null;
            logger.info("Sondeo detenido");
        }
    }

    public synchronized boolean isActivo() {
        return tarea != null && !tarea.isCancelled();
    }

    /**
     * Cuerpo de la tarea programada. Nunca propaga: si dejara escapar una excepcion, el
     * planificador cancelaria la tarea y el sondeo no volveria hasta reiniciar la aplicacion.
     */
    void ejecutarSondeo() {
        sondear(false);
    }

    /**
     * Sondeo a demanda: <b>ignora la ventana horaria</b> a proposito. Sirve para traer lo ultimo
     * que haya publicado la BVL en el momento de arrancar, aunque sea de madrugada o fin de semana
     * (en ese caso devuelve el cierre del ultimo dia habil).
     */
    void sondearAhora() {
        sondear(true);
    }

    private void sondear(boolean forzado) {
        // En hora de Lima, no en la del reloj: la ventana es la del mercado, este donde este el equipo.
        LocalTime ahora = LocalTime.ofInstant(clock.instant(), HorarioSondeo.ZONA_MERCADO);
        if (!forzado && !admiteDisparo(ahora)) {
            logger.debug("Disparo a las {} fuera de la ventana {} - {}: se omite", ahora,
                    horario.getInicio(), horario.getFin());
            return;
        }
        try {
            listener.onSondeoCompletado(ciclo.process());
        } catch (Throwable e) {
            logger.error("Fallo el sondeo de las {}: {}", ahora, e.getMessage(), e);
            listener.onSondeoFallido(e);
        }
    }

    /** Dentro de la ventana, o recien pasada su hora de fin por el retraso normal de un disparo. */
    private boolean admiteDisparo(LocalTime ahora) {
        return horario.dentroDeVentana(ahora)
                || horario.dentroDeVentana(ahora.minus(TOLERANCIA_RETRASO));
    }
}
