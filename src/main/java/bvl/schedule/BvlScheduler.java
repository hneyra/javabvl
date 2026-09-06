package bvl.schedule;

import bvl.config.BvlProperties;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalTime;
import java.util.concurrent.ScheduledFuture;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.stereotype.Service;

/**
 * Programa el sondeo de la BVL con un cron derivado de las propiedades, en lugar del hilo con
 * {@code sleep} que habia antes.
 *
 * <p>El cron cubre la rejilla del reloj dentro del rango de horas; los disparos que caen fuera del
 * horario real de sesion los descarta {@link HorarioSondeo#dentroDeVentana(LocalTime)}. Ver
 * {@link HorarioSondeo} para el porque de las dos piezas.
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
        this(ciclo, taskScheduler, properties.getHorario(), Clock.systemDefaultZone());
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
        // primer disparo del cron. Va al hilo del planificador, nunca al EDT: es red, BD y XLS.
        // El pool es de un solo hilo, asi que no puede solaparse con los disparos del cron.
        taskScheduler.schedule(this::sondearAhora, Instant.now(clock));
    }

    /**
     * Cambia el horario sin reiniciar la aplicacion. Si hay sondeo en marcha, se cancela la tarea
     * y se programa otra con el cron nuevo; si esta en reposo, el horario queda listo para el
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
        String cron = horario.toCron();
        logger.info("Programando sondeo: {} (ventana {} - {})", cron, horario.getInicio(),
                horario.getFin());
        return taskScheduler.schedule(this::ejecutarSondeo, new CronTrigger(cron));
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
        LocalTime ahora = LocalTime.now(clock);
        if (!forzado && !horario.dentroDeVentana(ahora)) {
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
}
