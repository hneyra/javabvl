package bvl.export;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Nombres de fichero y de hoja.
 *
 * <p>Estos nombres son el indice del historico: los XLS de anios anteriores ya estan en disco con
 * este patron y el usuario navega por el a mano. Cambiarlos parte la coleccion en dos.
 */
class RutaXlsTest {

  private static final String RAIZ = "raiz" + File.separator;

  private final RutaXls rutas = new RutaXls(RAIZ);

  private static String ruta(String... tramos) {
    return String.join(File.separator, tramos);
  }

  @Test
  @DisplayName("el fichero diario cuelga de <raiz>/<anio>/<Mes>")
  void ficheroDiario() {
    File fichero = rutas.ficheroDiario(LocalDateTime.of(2024, 1, 15, 10, 30));

    assertThat(fichero.getPath()).isEqualTo(ruta("raiz", "2024", "Enero", "2024.01.15.xls"));
  }

  @Test
  @DisplayName("el fichero mensual cuelga de <raiz>/<anio> y lleva el mes en el nombre")
  void ficheroMensual() {
    File fichero = rutas.ficheroMensual(LocalDateTime.of(2024, 1, 15, 10, 30));

    assertThat(fichero.getPath()).isEqualTo(ruta("raiz", "2024", "2024.01_Enero.xls"));
  }

  @Test
  @DisplayName("los meses y dias de un digito se rellenan con cero")
  void rellenoDeCeros() {
    LocalDateTime fecha = LocalDateTime.of(2024, 3, 7, 9, 5, 3);

    assertThat(rutas.ficheroDiario(fecha).getName()).isEqualTo("2024.03.07.xls");
    assertThat(rutas.ficheroMensual(fecha).getName()).isEqualTo("2024.03_Marzo.xls");
    assertThat(rutas.hojaDiaria(fecha)).isEqualTo("09.05.03");
    assertThat(rutas.hojaMensual(fecha)).isEqualTo("07");
  }

  @Test
  @DisplayName("los meses de dos digitos no se rellenan")
  void mesesDeDosDigitos() {
    LocalDateTime fecha = LocalDateTime.of(2024, 12, 31, 16, 30, 0);

    assertThat(rutas.ficheroDiario(fecha).getName()).isEqualTo("2024.12.31.xls");
    assertThat(rutas.ficheroMensual(fecha).getName()).isEqualTo("2024.12_Diciembre.xls");
    assertThat(rutas.hojaMensual(fecha)).isEqualTo("31");
  }

  @Test
  @DisplayName("septiembre se escribe 'Setiembre', como en el historico")
  void setiembreSeEscribeSinP() {
    // No sale de un formateador con locale: es el literal que llevan los ficheros ya generados.
    assertThat(rutas.ficheroMensual(LocalDateTime.of(2024, 9, 2, 10, 0)).getName())
        .isEqualTo("2024.09_Setiembre.xls");
  }

  @Test
  @DisplayName("la hoja diaria es la hora del sondeo con puntos")
  void hojaDiaria() {
    assertThat(rutas.hojaDiaria(LocalDateTime.of(2024, 1, 15, 16, 30, 45)))
        .isEqualTo("16.30.45");
  }

  @Test
  @DisplayName("la marca de tiempo de la cabecera lleva fecha y hora completas")
  void marcaDeTiempo() {
    assertThat(RutaXls.marcaDeTiempo(LocalDateTime.of(2024, 1, 15, 10, 30, 0)))
        .isEqualTo("2024-01-15 10:30:00");
  }
}
