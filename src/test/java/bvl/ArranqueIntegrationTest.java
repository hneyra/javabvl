package bvl;

import static org.assertj.core.api.Assertions.assertThat;

import bvl.alert.AlertaFormatter;
import bvl.alert.DetectorVariaciones;
import bvl.config.BvlProperties;
import bvl.market.BvlClient;
import bvl.market.CotizacionMapper;
import bvl.market.LectorBvl;
import bvl.schedule.BvlScheduler;
import bvl.schedule.CicloSondeo;
import bvl.service.ExportService;
import bvl.ui.VentanaPrincipal;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;

/**
 * Levanta el contexto entero de la aplicacion.
 *
 * <p>Es la unica prueba que recorre el cableado completo. Los demas tests montan contextos
 * recortados o construyen los objetos a mano, asi que sin esto un bean sin declarar, una dependencia
 * circular o un {@code @Value} mal escrito no se verian hasta ejecutar el jar en la maquina de
 * despliegue.
 *
 * <p>La ventana no entra: la suite corre headless y {@code SwingConfiguration} solo se activa si
 * hay pantalla. Que el resto arranque sin ella es justamente lo que se comprueba aqui.
 *
 * <p>Nota: al arrancar el contexto se ejecuta {@code TlsInseguro}, que desactiva la validacion de
 * certificados de {@code HttpsURLConnection} en esta JVM. Es lo que hace la aplicacion real y
 * ningun test de la suite hace peticiones HTTPS.
 */
@SpringBootTest
class ArranqueIntegrationTest {

  @Autowired
  ApplicationContext context;

  @Test
  @DisplayName("el contexto completo arranca con las propiedades de desarrollo")
  void elContextoArranca() {
    assertThat(context).isNotNull();
  }

  @Test
  @DisplayName("estan todos los colaboradores de un ciclo de sondeo")
  void colaboradoresDelCiclo() {
    assertThat(context.getBean(CicloSondeo.class)).isNotNull();
    assertThat(context.getBean(LectorBvl.class)).isNotNull();
    assertThat(context.getBean(BvlClient.class)).isNotNull();
    assertThat(context.getBean(CotizacionMapper.class)).isNotNull();
    assertThat(context.getBean(ExportService.class)).isNotNull();
  }

  @Test
  @DisplayName("estan el planificador y las alertas")
  void restoDeBeans() {
    assertThat(context.getBean(BvlScheduler.class)).isNotNull();
    assertThat(context.getBean(DetectorVariaciones.class)).isNotNull();
    assertThat(context.getBean(AlertaFormatter.class)).isNotNull();
  }

  @Test
  @DisplayName("las propiedades llegan tipadas y validadas")
  void propiedadesCargadas() {
    BvlProperties properties = context.getBean(BvlProperties.class);

    assertThat(properties.getUrlCotizaciones()).endsWith("/v1/stock-quote/market");
    assertThat(properties.getUrlHora()).endsWith("/v1/traded-amount/daily");
    assertThat(properties.getHorario().getInicio()).isEqualTo(java.time.LocalTime.of(9, 40));
    assertThat(properties.getHorario().getIntervaloMinutos()).isEqualTo(5);
    assertThat(properties.getAlarma()).isEqualTo("2");
  }

  @Test
  @DisplayName("la aplicacion arranca en reposo: no se sondea hasta pulsar Iniciar")
  void arrancaEnReposo() {
    assertThat(context.getBean(BvlScheduler.class).isActivo()).isFalse();
  }

  @Test
  @DisplayName("sin pantalla no se construye la ventana, y el resto del contexto no la echa en falta")
  void sinPantallaNoHayVentana() {
    assertThat(context.getBeanNamesForType(VentanaPrincipal.class)).isEmpty();
  }
}
