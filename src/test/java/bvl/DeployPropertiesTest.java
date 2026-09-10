package bvl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import bvl.schedule.HorarioSondeo;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.Properties;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Blindaje de {@code deploy/bvl.properties}, el fichero que se pasa con
 * {@code --spring.config.location}.
 *
 * <p>Ese flag **sustituye** al {@code application.properties} empaquetado en lugar de
 * complementarlo, asi que una propiedad sin valor por defecto que falte alli tumba el arranque en
 * produccion y en ningun otro sitio. Este test lo detecta en el build.
 *
 * <p>Todas las propiedades se declaran en un unico sitio, {@code bvl.config.BvlProperties}. Si
 * anades alli una sin valor por defecto, anadela tambien a esta lista y al fichero de despliegue.
 */
class DeployPropertiesTest {

  /** Propiedades que el codigo exige sin valor por defecto. */
  private static final String[] OBLIGATORIAS = {
      "baseUrl", "urlCotizaciones", "urlHora", "xlsPath",
      "alarma", "horaInicio", "horaFin", "intervalo",
  };

  private static Properties cargar(String ruta) throws IOException {
    Path p = Path.of(ruta);
    assertThat(p).as("fichero %s", ruta).exists();
    Properties props = new Properties();
    try (InputStream in = Files.newInputStream(p)) {
      props.load(in);
    }
    return props;
  }

  @Test
  @DisplayName("deploy/bvl.properties declara todo lo que el codigo exige sin default")
  void despliegueDeclaraLasObligatorias() throws IOException {
    Properties props = cargar("deploy/bvl.properties");

    assertThat(props.stringPropertyNames()).containsAll(java.util.List.of(OBLIGATORIAS));
  }

  @Test
  @DisplayName("el horario de despliegue es valido y sondea a las horas esperadas")
  void horarioDeDespliegueEsValido() throws IOException {
    Properties props = cargar("deploy/bvl.properties");

    HorarioSondeo horario = HorarioSondeo.of(
        props.getProperty("horaInicio"),
        props.getProperty("horaFin"),
        props.getProperty("intervalo"));

    LocalDateTime primero = horario.siguienteSondeo(LocalDateTime.of(2026, 9, 8, 8, 0));
    assertThat(primero).isEqualTo(LocalDateTime.of(2026, 9, 8, 9, 40));
    assertThat(horario.siguienteSondeo(primero)).isEqualTo(LocalDateTime.of(2026, 9, 8, 10, 0));
  }

  @Test
  @DisplayName("alertaTimeout de despliegue se parsea y vale 10 minutos")
  void alertaTimeoutDeDespliegue() throws IOException {
    Properties props = cargar("deploy/bvl.properties");
    String valor = props.getProperty("alertaTimeout");

    assertThat(valor).as("alertaTimeout en deploy/bvl.properties").isNotNull();
    assertThat(HorarioSondeo.parseDuracion(valor, "alertaTimeout"))
        .isEqualTo(java.time.Duration.ofMinutes(10));
  }

  @Test
  @DisplayName("el properties de desarrollo tambien arranca con un horario valido")
  void horarioDeDesarrolloEsValido() throws IOException {
    Properties props = cargar("src/main/resources/application.properties");

    assertThatCode(() -> HorarioSondeo.of(
        props.getProperty("horaInicio"),
        props.getProperty("horaFin"),
        props.getProperty("intervalo"))).doesNotThrowAnyException();
    assertThat(HorarioSondeo.parseDuracion(props.getProperty("alertaTimeout"), "alertaTimeout"))
        .isEqualTo(java.time.Duration.ofMinutes(10));
  }
}
