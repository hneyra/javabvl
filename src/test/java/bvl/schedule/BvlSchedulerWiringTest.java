package bvl.schedule;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import bvl.config.BvlProperties;
import bvl.config.SchedulingConfiguration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.scheduling.TaskScheduler;

/**
 * Cableado real con Spring: que las propiedades lleguen al scheduler y que un horario invalido
 * tumbe el arranque.
 *
 * <p>Los demas tests del scheduler usan mocks, asi que sin esto una propiedad mal escrita o un
 * {@code TaskScheduler} sin declarar no se veria hasta ejecutar la aplicacion.
 *
 * <p>El mock del ciclo se registra con {@code registerSingleton} y no con {@code withBean}: creado
 * por el contenedor arrastraria a sus colaboradores reales (lector, servicios, repositorios).
 *
 * <p>El {@link PropertySourcesPlaceholderConfigurer} es imprescindible y no es adorno: sin el, un
 * contexto de test resuelve los placeholders con {@code Environment.resolvePlaceholders}, que deja
 * {@code ${loQueFalte}} como literal en vez de fallar. Boot si lo registra en produccion, asi que
 * sin este bean el test seria mas permisivo que la aplicacion real y dejaria pasar justo el fallo
 * que busca.
 */
class BvlSchedulerWiringTest {

  /** Lo que no se esta probando aqui, con valores validos cualesquiera. */
  private static final String[] RESTO_DE_PROPIEDADES = {
      "baseUrl=http://localhost",
      "urlCotizaciones=http://localhost/v1/stock-quote/market",
      "urlHora=http://localhost/v1/traded-amount/daily",
      "xlsPath=${java.io.tmpdir}/javabvl-tests/",
      "alarma=2",
  };

  private final ApplicationContextRunner runner = new ApplicationContextRunner()
      .withBean(PropertySourcesPlaceholderConfigurer.class)
      .withUserConfiguration(SchedulingConfiguration.class, BvlProperties.class,
          BvlScheduler.class)
      .withInitializer(ctx -> ctx.getBeanFactory()
          .registerSingleton("cicloSondeo", mock(CicloSondeo.class)))
      .withPropertyValues(RESTO_DE_PROPIEDADES);

  @Test
  @DisplayName("los valores de produccion (deploy/bvl.properties) levantan el contexto")
  void cableadoConValoresDeProduccion() {
    runner.withPropertyValues("horaInicio=9:40:00", "horaFin=16:30", "intervalo=00:20:00")
        .run(context -> {
          assertThat(context).hasNotFailed();
          assertThat(context).hasSingleBean(TaskScheduler.class);
          assertThat(context.getBean(BvlScheduler.class).getHorario().getIntervaloMinutos())
              .isEqualTo(20);
        });
  }

  @Test
  @DisplayName("los valores de desarrollo (application.properties) levantan el contexto")
  void cableadoConValoresDeDesarrollo() {
    runner.withPropertyValues("horaInicio=9:40:00", "horaFin=16:30", "intervalo=00:05:00")
        .run(context -> {
          assertThat(context).hasNotFailed();
          assertThat(context.getBean(BvlScheduler.class).getHorario().getIntervaloMinutos())
              .isEqualTo(5);
        });
  }

  @Test
  @DisplayName("el scheduler arranca en reposo: nada se programa hasta pulsar Iniciar")
  void arrancaEnReposo() {
    runner.withPropertyValues("horaInicio=9:40:00", "horaFin=16:30", "intervalo=00:20:00")
        .run(context -> assertThat(context.getBean(BvlScheduler.class).isActivo()).isFalse());
  }

  @Test
  @DisplayName("alertaTimeout tiene valor por defecto: su ausencia no impide arrancar")
  void alertaTimeoutTieneDefecto() {
    // --spring.config.location sustituye al properties empaquetado, asi que toda propiedad sin
    // default debe estar en deploy/bvl.properties. Esta tiene default a proposito.
    runner.withPropertyValues("horaInicio=9:40:00", "horaFin=16:30", "intervalo=00:20:00")
        .run(context -> {
          assertThat(context).hasNotFailed();
          assertThat(context.getBean(BvlProperties.class).getAlertaTimeout())
              .isEqualTo("00:10:00");
        });
  }

  @Test
  @DisplayName("una propiedad obligatoria que falta impide arrancar")
  void propiedadObligatoriaAusenteTumbaElArranque() {
    new ApplicationContextRunner()
        .withBean(PropertySourcesPlaceholderConfigurer.class)
        .withUserConfiguration(BvlProperties.class)
        .withPropertyValues("horaInicio=9:40:00", "horaFin=16:30", "intervalo=00:20:00")
        .run(context -> assertThat(context).hasFailed());
  }

  @Test
  @DisplayName("un intervalo invalido impide arrancar, con el nombre de la propiedad en el error")
  void intervaloInvalidoTumbaElArranque() {
    // Mejor no arrancar que sondear con una cadencia distinta de la configurada.
    runner.withPropertyValues("horaInicio=9:40:00", "horaFin=16:30", "intervalo=00:07:30")
        .run(context -> {
          assertThat(context).hasFailed();
          assertThat(context.getStartupFailure())
              .hasRootCauseInstanceOf(IllegalArgumentException.class);
          assertThat(context.getStartupFailure()).rootCause()
              .hasMessageContaining("intervalo")
              .hasMessageContaining("minutos enteros");
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
