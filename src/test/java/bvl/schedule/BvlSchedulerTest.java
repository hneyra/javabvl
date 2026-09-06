package bvl.schedule;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.concurrent.ScheduledFuture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.Trigger;
import org.springframework.scheduling.support.CronTrigger;

/**
 * Planificacion del sondeo: programacion en el TaskScheduler y guarda de ventana horaria.
 *
 * <p>No se arranca ningun hilo real: se invoca {@code ejecutarSondeo()} directamente con un reloj
 * fijo, que es lo que hace la tarea en cada disparo del cron.
 */
class BvlSchedulerTest {

  private static final ZoneId ZONA = ZoneId.of("America/Lima");

  private CicloSondeo ciclo;
  private TaskScheduler taskScheduler;
  private SondeoListener listener;

  @BeforeEach
  void setUp() {
    ciclo = mock(CicloSondeo.class);
    taskScheduler = mock(TaskScheduler.class);
    listener = mock(SondeoListener.class);
  }

  /** Scheduler con el reloj clavado a una hora concreta del martes 2024-01-16. */
  private BvlScheduler schedulerA(LocalTime hora) {
    Clock reloj = Clock.fixed(
        LocalDateTime.of(2024, 1, 16, hora.getHour(), hora.getMinute()).atZone(ZONA).toInstant(),
        ZONA);
    BvlScheduler s = new BvlScheduler(ciclo, taskScheduler,
        HorarioSondeo.of("9:40:00", "16:30", "00:20:00"), reloj);
    s.setListener(listener);
    return s;
  }

  /** El planificador devuelve siempre el mismo futuro, para poder verificar su cancelacion. */
  private ScheduledFuture<?> programacionDevuelve() {
    ScheduledFuture<?> future = mock(ScheduledFuture.class);
    when(taskScheduler.schedule(any(Runnable.class), any(Trigger.class))).thenAnswer(i -> future);
    return future;
  }

  @Test
  @DisplayName("dentro de la ventana ejecuta el sondeo y avisa al listener")
  void dentroDeVentanaSondea() {
    schedulerA(LocalTime.of(12, 0)).ejecutarSondeo();

    verify(ciclo).process();
    verify(listener).onSondeoCompletado(any());
  }

  @Test
  @DisplayName("el resultado del ciclo llega al listener tal cual, sin releer nada")
  void elResultadoViajaAlListener() {
    // Antes las cotizaciones vivian en un campo del orquestador que leia el EDT sin sincronizar,
    // y la ventana pedia la fecha otra vez por HTTP solo para el titulo del aviso.
    ResultadoSondeo resultado = new ResultadoSondeo(
        new Lectura(List.of(), LocalDateTime.of(2024, 1, 16, 12, 0)), null);
    when(ciclo.process()).thenReturn(resultado);

    schedulerA(LocalTime.of(12, 0)).ejecutarSondeo();

    verify(listener).onSondeoCompletado(resultado);
  }

  @Test
  @DisplayName("los extremos de la ventana tambien sondean")
  void extremosDeVentanaSondean() {
    schedulerA(LocalTime.of(9, 40)).ejecutarSondeo();
    schedulerA(LocalTime.of(16, 30)).ejecutarSondeo();

    verify(ciclo, times(2)).process();
  }

  @Test
  @DisplayName("un disparo del cron antes de la apertura no toca la BVL")
  void antesDeLaAperturaNoSondea() {
    // El cron dispara a las 9:20 porque la hora 9 entra entera en el rango 9-16.
    schedulerA(LocalTime.of(9, 20)).ejecutarSondeo();

    verifyNoInteractions(ciclo);
    verify(listener, never()).onSondeoCompletado(any());
  }

  @Test
  @DisplayName("un disparo del cron tras el cierre no toca la BVL")
  void trasElCierreNoSondea() {
    schedulerA(LocalTime.of(16, 40)).ejecutarSondeo();

    verifyNoInteractions(ciclo);
  }

  @Test
  @DisplayName("un fallo del sondeo se notifica y no se propaga, para no matar la tarea")
  void elFalloNoTumbaLaTarea() {
    BvlScheduler s = schedulerA(LocalTime.of(12, 0));
    RuntimeException boom = new RuntimeException("sin conexion con la BVL");
    doThrow(boom).when(ciclo).process();

    s.ejecutarSondeo();

    verify(listener).onSondeoFallido(boom);
    verify(listener, never()).onSondeoCompletado(any());
  }

  @Test
  @DisplayName("iniciar programa la tarea con el cron del horario")
  void iniciarProgramaElCron() {
    programacionDevuelve();
    BvlScheduler s = schedulerA(LocalTime.of(12, 0));

    s.iniciar();

    ArgumentCaptor<Trigger> captor = ArgumentCaptor.forClass(Trigger.class);
    verify(taskScheduler).schedule(any(Runnable.class), captor.capture());
    assertThat(captor.getValue()).isInstanceOf(CronTrigger.class);
    assertThat(((CronTrigger) captor.getValue()).getExpression())
        .isEqualTo("0 0/20 9-16 * * MON-FRI");
    assertThat(s.isActivo()).isTrue();
  }

  @Test
  @DisplayName("iniciar dos veces no duplica la tarea")
  void iniciarEsIdempotente() {
    programacionDevuelve();
    BvlScheduler s = schedulerA(LocalTime.of(12, 0));

    s.iniciar();
    s.iniciar();

    verify(taskScheduler, times(1)).schedule(any(Runnable.class), any(Trigger.class));
  }

  @Test
  @DisplayName("detener cancela la tarea y deja el scheduler inactivo")
  void detenerCancela() {
    ScheduledFuture<?> future = programacionDevuelve();
    BvlScheduler s = schedulerA(LocalTime.of(12, 0));
    s.iniciar();

    s.detener();

    verify(future).cancel(false);
    assertThat(s.isActivo()).isFalse();
  }

  @Test
  @DisplayName("detener sin haber iniciado no falla")
  void detenerSinIniciarEsInocuo() {
    BvlScheduler s = schedulerA(LocalTime.of(12, 0));

    s.detener();

    assertThat(s.isActivo()).isFalse();
    verifyNoInteractions(taskScheduler);
  }

  @Test
  @DisplayName("iniciar lanza una lectura inmediata, sin esperar al primer disparo del cron")
  void iniciarLanzaLecturaInmediata() {
    BvlScheduler s = schedulerA(LocalTime.of(12, 0));

    s.iniciar();

    // La lectura va al hilo del planificador, no al EDT: es red + BD + escritura de XLS.
    ArgumentCaptor<Runnable> captor = ArgumentCaptor.forClass(Runnable.class);
    verify(taskScheduler).schedule(captor.capture(), any(Instant.class));
    verifyNoInteractions(ciclo);

    captor.getValue().run();

    verify(ciclo).process();
    verify(listener).onSondeoCompletado(any());
  }

  @Test
  @DisplayName("la lectura inmediata ignora la ventana: trae los ultimos datos publicados")
  void lecturaInmediataIgnoraLaVentana() {
    // A las 3 de la madrugada el cron no sondearia, pero pulsar Iniciar debe leer igual.
    schedulerA(LocalTime.of(3, 0)).sondearAhora();

    verify(ciclo).process();
    verify(listener).onSondeoCompletado(any());
  }

  @Test
  @DisplayName("el cron sigue respetando la ventana aunque la lectura inmediata no")
  void elCronSigueRespetandoLaVentana() {
    schedulerA(LocalTime.of(3, 0)).ejecutarSondeo();

    verifyNoInteractions(ciclo);
  }

  @Test
  @DisplayName("un fallo en la lectura inmediata se notifica y no se propaga")
  void falloEnLecturaInmediataNoSePropaga() {
    BvlScheduler s = schedulerA(LocalTime.of(3, 0));
    RuntimeException boom = new RuntimeException("sin conexion con la BVL");
    doThrow(boom).when(ciclo).process();

    s.sondearAhora();

    verify(listener).onSondeoFallido(boom);
  }

  @Test
  @DisplayName("iniciar sobre un scheduler ya activo no repite la lectura inmediata")
  void iniciarActivoNoRepiteLecturaInmediata() {
    programacionDevuelve();
    BvlScheduler s = schedulerA(LocalTime.of(12, 0));

    s.iniciar();
    s.iniciar();

    verify(taskScheduler, times(1)).schedule(any(Runnable.class), any(Instant.class));
  }

  @Test
  @DisplayName("reprogramar en marcha cancela la tarea vieja y programa el cron nuevo")
  void reprogramarEnMarcha() {
    ScheduledFuture<?> vieja = mock(ScheduledFuture.class);
    ScheduledFuture<?> nueva = mock(ScheduledFuture.class);
    when(taskScheduler.schedule(any(Runnable.class), any(Trigger.class)))
        .thenAnswer(i -> vieja).thenAnswer(i -> nueva);
    BvlScheduler s = schedulerA(LocalTime.of(12, 0));
    s.iniciar();

    s.reprogramar(HorarioSondeo.of("10:00:00", "15:00", "00:05:00"));

    verify(vieja).cancel(false);
    ArgumentCaptor<Trigger> captor = ArgumentCaptor.forClass(Trigger.class);
    verify(taskScheduler, times(2)).schedule(any(Runnable.class), captor.capture());
    assertThat(((CronTrigger) captor.getAllValues().get(1)).getExpression())
        .isEqualTo("0 0/5 10-15 * * MON-FRI");
    assertThat(s.isActivo()).isTrue();
  }

  @Test
  @DisplayName("reprogramar en reposo cambia el horario sin programar nada")
  void reprogramarEnReposo() {
    BvlScheduler s = schedulerA(LocalTime.of(12, 0));

    s.reprogramar(HorarioSondeo.of("10:00:00", "15:00", "00:05:00"));

    verifyNoInteractions(taskScheduler);
    assertThat(s.isActivo()).isFalse();
    assertThat(s.getHorario().toCron()).isEqualTo("0 0/5 10-15 * * MON-FRI");
  }

  @Test
  @DisplayName("la ventana nueva manda inmediatamente sobre la guarda")
  void reprogramarCambiaLaGuardaDeVentana() {
    // A las 12:00 la ventana vieja (9:40-16:30) sondearia; la nueva (9:00-11:00) ya no.
    BvlScheduler s = schedulerA(LocalTime.of(12, 0));
    s.reprogramar(HorarioSondeo.of("9:00:00", "11:00", "00:05:00"));

    s.ejecutarSondeo();

    verifyNoInteractions(ciclo);
  }

  @Test
  @DisplayName("sin listener registrado el sondeo sigue funcionando")
  void sinListenerNoRevienta() {
    Clock reloj = Clock.fixed(Instant.parse("2024-01-16T17:00:00Z"), ZONA);
    BvlScheduler s = new BvlScheduler(ciclo, taskScheduler,
        HorarioSondeo.of("9:40:00", "16:30", "00:20:00"), reloj);

    s.ejecutarSondeo();

    verify(ciclo).process();
  }
}
