package bvl.alert;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Redacta la alerta en los dos formatos que hacen falta: texto plano para el globo de la bandeja y
 * HTML para el dialogo.
 *
 * <p>TRAMPA CONOCIDA: el numero se formatea con el {@code Locale} por defecto de la JVM, asi que el
 * separador decimal es coma o punto segun la maquina donde corra. Se conserva tal cual; ver
 * "Trampas conocidas" en CLAUDE.md.
 */
@Component
public class AlertaFormatter {

    public static final String SIN_VARIACIONES = "Sin variaciones.";

    private static final String ENCABEZADO = "<b>Las siguientes empresas variaron:</b><br /><br />";
    private static final int DECIMALES = 2;

    /** Una linea por accion, para el globo de la bandeja. */
    public String texto(List<Variacion> variaciones) {
        if (variaciones.isEmpty()) {
            return SIN_VARIACIONES;
        }
        StringBuilder texto = new StringBuilder();
        for (Variacion variacion : variaciones) {
            texto.append(linea(variacion)).append('\n');
        }
        return texto.toString();
    }

    /** Lo mismo en HTML, con las subidas en azul y las bajadas en rojo. */
    public String html(List<Variacion> variaciones) {
        if (variaciones.isEmpty()) {
            return SIN_VARIACIONES;
        }
        StringBuilder html = new StringBuilder("<html>").append(ENCABEZADO);
        for (Variacion variacion : variaciones) {
            html.append("<div style='color:")
                    .append(variacion.esSubida() ? "blue" : "red")
                    .append("'>")
                    .append(linea(variacion))
                    .append("</div>");
        }
        return html.append("</html>").toString();
    }

    private static String linea(Variacion variacion) {
        return (variacion.esSubida() ? " + " : " - ")
                + variacion.nemonico()
                + (variacion.esSubida() ? " subió " : " bajó ")
                + formato().format(variacion.porcentaje())
                + "%";
    }

    /**
     * Se crea en cada llamada a proposito: {@link NumberFormat} no es thread-safe y, ademas, un
     * formateador estatico congelaria el {@code Locale} en el momento de cargar la clase.
     */
    private static NumberFormat formato() {
        NumberFormat formato = DecimalFormat.getInstance();
        formato.setMaximumFractionDigits(DECIMALES);
        return formato;
    }
}
