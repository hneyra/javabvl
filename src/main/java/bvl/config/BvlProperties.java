package bvl.config;

import bvl.schedule.HorarioSondeo;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Punto unico donde aparece el nombre de cada propiedad de configuracion.
 *
 * <p>Antes estaban repartidas en seis clases mediante {@code @Value} sobre campos, asi que para
 * saber que espera la aplicacion habia que rastrear todo el codigo. Eso importa mas de lo normal
 * aqui: {@code --spring.config.location} <b>sustituye</b> al {@code application.properties}
 * empaquetado en lugar de complementarlo, de modo que una propiedad sin valor por defecto que falte
 * en {@code deploy/bvl.properties} solo revienta en produccion. {@code DeployPropertiesTest} vigila
 * esa lista contra este fichero.
 *
 * <p>El horario se construye aqui, al arrancar, para que una configuracion invalida impida levantar
 * la aplicacion en vez de fallar a mitad de sesion.
 *
 * <p><b>Dos propiedades se exponen en crudo a proposito</b>, no convertidas:
 * <ul>
 *   <li>{@code alarma} viaja como texto porque se vuelca tal cual en el campo de la ventana:
 *       convertirla a {@code double} mostraria "2.0" donde el usuario lleva viendo "2".
 *   <li>{@code alertaTimeout} viaja como texto porque la UI lo parsea tarde y, si no vale, avisa en
 *       la barra de estado y sigue con 10 minutos. Parsearlo aqui convertiria esa degradacion
 *       elegante en un arranque fallido.
 * </ul>
 */
@Component
public class BvlProperties {

    private final String baseUrl;
    private final String urlCotizaciones;
    private final String urlHora;
    private final String xlsPath;
    private final String alarma;
    private final String alertaTimeout;
    private final HorarioSondeo horario;

    public BvlProperties(
            @Value("${baseUrl}") String baseUrl,
            @Value("${urlCotizaciones}") String urlCotizaciones,
            @Value("${urlHora}") String urlHora,
            @Value("${xlsPath}") String xlsPath,
            @Value("${alarma}") String alarma,
            @Value("${alertaTimeout:00:10:00}") String alertaTimeout,
            @Value("${horaInicio}") String horaInicio,
            @Value("${horaFin}") String horaFin,
            @Value("${intervalo}") String intervalo) {
        this.baseUrl = baseUrl;
        this.urlCotizaciones = urlCotizaciones;
        this.urlHora = urlHora;
        this.xlsPath = xlsPath;
        this.alarma = alarma;
        this.alertaTimeout = alertaTimeout;
        this.horario = HorarioSondeo.of(horaInicio, horaFin, intervalo);
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public String getUrlCotizaciones() {
        return urlCotizaciones;
    }

    public String getUrlHora() {
        return urlHora;
    }

    /** Raiz donde se crea el arbol {@code <anio>/<Mes>/<fichero>.xls}. Acaba en separador. */
    public String getXlsPath() {
        return xlsPath;
    }

    /** Umbral de variacion en porcentaje, en crudo: se muestra en el campo de la ventana. */
    public String getAlarma() {
        return alarma;
    }

    /** Timeout de la alerta en crudo ({@code hh:mm:ss}); lo interpreta la UI. */
    public String getAlertaTimeout() {
        return alertaTimeout;
    }

    /** Horario de sondeo inicial, ya validado. La ventana puede reprogramarlo en caliente. */
    public HorarioSondeo getHorario() {
        return horario;
    }

    /** Duracion por defecto de la alerta cuando {@link #getAlertaTimeout()} no se puede leer. */
    public static Duration alertaTimeoutPorDefecto() {
        return Duration.ofMinutes(10);
    }
}
