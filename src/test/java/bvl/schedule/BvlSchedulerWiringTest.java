package bvl.schedule;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import bvl.BVL2;
import bvl.config.SchedulingConfiguration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.scheduling.TaskScheduler;

/**
 * Cableado real con Spring: que las propiedades lleguen al scheduler y que un horario invalido
 * tumbe el arranque.
 *
 * <p>Los demas tests del scheduler usan mocks, asi que sin esto un {@code @Value} mal escrito o un
 * {@code TaskScheduler} sin declarar no se veria hasta ejecutar la aplicacion.
 */
class BvlSchedulerWiringTest {

  // registerSingleton y no withBean: un mock de BVL2 creado por el contenedor pasaria por el
  // autowiring de sus campos @Autowired y arrastraria BvlReader, BvlService y los repositorios.
  private final ApplicationContextRunner runner = new ApplicationContextRunner()
      .withUserConfiguration(SchedulingConfiguration.class, BvlScheduler.class)
      .withInitializer(ctx -> ctx.getBeanFactory().registerSingleton("bvl2", mock(BVL2.class)));

  @Test
  @DisplayName("los valores de produccion (deploy/bvl.properties) levantan el contexto")
  void cableadoConValoresDeProduccion() {
    runner.withPropertyValues("horaInicio=9:40:00", "horaFin=16:30", "intervalo=00:20:00")
        .run(context -> {
          assertThat(context).hasNotFailed();
          assertThat(context).hasSingleBean(TaskScheduler.class);
          assertThat(context.getBean(BvlScheduler.class).getHorario().toCron())
              .isEqualTo("0 0/20 9-16 * * MON-FRI");
        });
  }

  @Test
  @DisplayName("los valores de desarrollo (application.properties) levantan el contexto")
  void cableadoConValoresDeDesarrollo() {
    runner.withPropertyValues("horaInicio=9:40:00", "horaFin=16:30", "intervalo=00:05:00")
        .run(context -> {
          assertThat(context).hasNotFailed();
          assertThat(context.getBean(BvlScheduler.class).getHorario().toCron())
              .isEqualTo("0 0/5 9-16 * * MON-FRI");
        });
  }

  @Test
  @DisplayName("el scheduler arranca en reposo: nada se programa hasta pulsar Iniciar")
  void arrancaEnReposo() {
    runner.withPropertyValues("horaInicio=9:40:00", "horaFin=16:30", "intervalo=00:20:00")
        .run(context -> assertThat(context.getBean(BvlScheduler.class).isActivo()).isFalse());
  }

  @Test
  @DisplayName("un intervalo invalido impide arrancar, con el nombre de la propiedad en el error")
  void intervaloInvalidoTumbaElArranque() {
    // Mejor no arrancar que sondear con una cadencia distinta de la configurada.
    runner.withPropertyValues("horaInicio=9:40:00", "horaFin=16:30", "intervalo=00:07:00")
        .run(context -> {
          assertThat(context).hasFailed();
          assertThat(context.getStartupFailure())
              .hasRootCauseInstanceOf(IllegalArgumentException.class);
          assertThat(context.getStartupFailure()).rootCause()
              .hasMessageContaining("intervalo")
              .hasMessageContaining("divisor");
        });
  }

  @Test
  @DisplayName("una ventana invertida impide arrancar")
  void ventanaInvalidaTumbaElArranque() {
    runner.withPropertyValues("horaInicio=16:30:00", "horaFin=9:40:00", "intervalo=00:20:00")
        .run(context -> {
          assertThat(context).hasFailed();
          assertThat(context.getStartupFailure()).rootCause()
              .hasMessageContaining("horaFin");
        });
  }
}
