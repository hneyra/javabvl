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

/**
 * Planificacion del sondeo: programacion en el TaskScheduler y guarda de ventana horaria.
 *
 * <p>No se arranca ningun hilo real: se invoca {@code ejecutarSondeo()} directamente con un reloj
 * fijo, que es lo que hace la tarea en cada disparo programado.
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
  @DisplayName("un disparo antes de la apertura no toca la BVL")
  void antesDeLaAperturaNoSondea() {
    // El disparador no programa nada fuera de horario, pero la guarda sigue ahi por si llega uno
    // (un reloj del sistema que se corrige, un disparo atrasado).
    schedulerA(LocalTime.of(9, 20)).ejecutarSondeo();

    verifyNoInteractions(ciclo);
    verify(listener, never()).onSondeoCompletado(any());
  }

  @Test
  @DisplayName("un disparo tras el cierre no toca la BVL")
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
  @DisplayName("iniciar programa la tarea con un disparo anclado al horario")
  void iniciarProgramaElDisparo() {
    programacionDevuelve();
    BvlScheduler s = schedulerA(LocalTime.of(12, 0));

    s.iniciar();

    ArgumentCaptor<Trigger> captor = ArgumentCaptor.forClass(Trigger.class);
    verify(taskScheduler).schedule(any(Runnable.class), captor.capture());
    assertThat(captor.getValue()).isInstanceOf(DisparoAnclado.class);
    HorarioSondeo programado = ((DisparoAnclado) captor.getValue()).getHorario();
    assertThat(programado.getInicio()).isEqualTo(LocalTime.of(9, 40));
    assertThat(programado.getIntervaloMinutos()).isEqualTo(20);
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
  @DisplayName("iniciar lanza una lectura inmediata, sin esperar al primer disparo programado")
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
    // A las 3 de la madrugada no hay sondeo programado, pero pulsar Iniciar debe leer igual.
    schedulerA(LocalTime.of(3, 0)).sondearAhora();

    verify(ciclo).process();
    verify(listener).onSondeoCompletado(any());
  }

  @Test
  @DisplayName("los disparos programados respetan la ventana aunque la lectura inmediata no")
  void losDisparosRespetanLaVentana() {
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
  @DisplayName("reprogramar en marcha cancela la tarea vieja y programa el horario nuevo")
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
    HorarioSondeo nuevo = ((DisparoAnclado) captor.getAllValues().get(1)).getHorario();
    assertThat(nuevo.getInicio()).isEqualTo(LocalTime.of(10, 0));
    assertThat(nuevo.getIntervaloMinutos()).isEqualTo(5);
    assertThat(s.isActivo()).isTrue();
  }

  @Test
  @DisplayName("reprogramar en reposo cambia el horario sin programar nada")
  void reprogramarEnReposo() {
    BvlScheduler s = schedulerA(LocalTime.of(12, 0));

    s.reprogramar(HorarioSondeo.of("10:00:00", "15:00", "00:05:00"));

    verifyNoInteractions(taskScheduler);
    assertThat(s.isActivo()).isFalse();
    assertThat(s.getHorario().getInicio()).isEqualTo(LocalTime.of(10, 0));
    assertThat(s.getHorario().getIntervaloMinutos()).isEqualTo(5);
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

  // --- Guarda de ventana: zona y retrasos ---------------------------------------------------

  /** Scheduler con un reloj exacto, para probar zonas horarias y retrasos de milisegundos. */
  private BvlScheduler schedulerConReloj(Clock reloj) {
    BvlScheduler s = new BvlScheduler(ciclo, taskScheduler,
        HorarioSondeo.of("9:40:00", "16:30", "00:20:00"), reloj);
    s.setListener(listener);
    return s;
  }

  @Test
  @DisplayName("el sondeo de la hora de fin no se pierde aunque el disparo llegue milisegundos tarde")
  void sondeoDeCierreConRetraso() {
    // Un disparo programado a las 16:30:00 se ejecuta siempre un poco despues. Con la guarda
    // exacta, 16:30:00.005 ya quedaba fuera de la ventana y el sondeo de cierre no ocurria nunca.
    Instant tarde = LocalDateTime.of(2024, 1, 16, 16, 30, 0, 5_000_000).atZone(ZONA).toInstant();

    schedulerConReloj(Clock.fixed(tarde, ZONA)).ejecutarSondeo();

    verify(ciclo).process();
  }

  @Test
  @DisplayName("la ventana se mide en hora de Lima aunque el equipo este en otra zona")
  void ventanaEnHoraDeLima() {
    // 17:00 UTC de enero: las 12:00 en Lima, dentro de la sesion; las 18:00 en Madrid, fuera.
    Clock madrid = Clock.fixed(Instant.parse("2024-01-16T17:00:00Z"), ZoneId.of("Europe/Madrid"));

    schedulerConReloj(madrid).ejecutarSondeo();

    verify(ciclo).process();
  }

  @Test
  @DisplayName("un disparo muy tardio, con el equipo dormido hasta la noche, no sondea")
  void disparoMuyTardioNoSondea() {
    // Si el equipo se suspende a media sesion, el disparo pendiente se ejecuta al despertar.
    Instant noche = LocalDateTime.of(2024, 1, 16, 21, 0).atZone(ZONA).toInstant();

    schedulerConReloj(Clock.fixed(noche, ZONA)).ejecutarSondeo();

    verifyNoInteractions(ciclo);
  }
}
