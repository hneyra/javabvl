package bvl.alert;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Redacta la alerta en los dos formatos que hacen falta: texto plano para el globo de la bandeja y
 * HTML para el dialogo.
 *
 * <p>El aviso puede llevar dos bloques, y conviene no mezclarlos: primero las acciones que se han
 * movido respecto al <b>cierre de la sesion anterior</b> (el porcentaje que publica la BVL), y
 * despues, si hay lectura previa con la que comparar, las que se han movido <b>desde el sondeo
 * anterior</b> de la propia aplicacion.
 *
 * <p>Con el segundo bloque vacio el mensaje sale identico al de siempre, carater por caracter.
 *
 * <p>TRAMPA CONOCIDA: el numero se formatea con el {@code Locale} por defecto de la JVM, asi que el
 * separador decimal es coma o punto segun la maquina donde corra. Se conserva tal cual; ver
 * "Trampas conocidas" en CLAUDE.md.
 */
@Component
public class AlertaFormatter {

    public static final String SIN_VARIACIONES = "Sin variaciones.";

    private static final String ENCABEZADO = "<b>Las siguientes empresas variaron:</b><br /><br />";
    private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("HH:mm");
    private static final int DECIMALES = 2;

    /** Una linea por accion, para el globo de la bandeja. */
    public String texto(List<Variacion> contraCierreAnterior) {
        return texto(contraCierreAnterior, List.of(), null);
    }

    /**
     * @param desdeElSondeoAnterior movimiento intradia; vacio si no hay lectura previa
     * @param instanteAnterior      hora de esa lectura previa; se ignora si la lista viene vacia
     */
    public String texto(List<Variacion> contraCierreAnterior,
                        List<Variacion> desdeElSondeoAnterior, LocalDateTime instanteAnterior) {
        if (contraCierreAnterior.isEmpty() && desdeElSondeoAnterior.isEmpty()) {
            return SIN_VARIACIONES;
        }
        StringBuilder texto = new StringBuilder();
        for (Variacion variacion : contraCierreAnterior) {
            texto.append(linea(variacion)).append('\n');
        }
        if (!desdeElSondeoAnterior.isEmpty()) {
            if (!contraCierreAnterior.isEmpty()) {
                texto.append('\n');
            }
            texto.append("Desde las ").append(HORA.format(instanteAnterior)).append(":\n");
            for (Variacion variacion : desdeElSondeoAnterior) {
                texto.append(linea(variacion)).append('\n');
            }
        }
        return texto.toString();
    }

    /** Lo mismo en HTML, con las subidas en azul y las bajadas en rojo. */
    public String html(List<Variacion> contraCierreAnterior) {
        return html(contraCierreAnterior, List.of(), null);
    }

    public String html(List<Variacion> contraCierreAnterior,
                       List<Variacion> desdeElSondeoAnterior, LocalDateTime instanteAnterior) {
        if (contraCierreAnterior.isEmpty() && desdeElSondeoAnterior.isEmpty()) {
            return SIN_VARIACIONES;
        }
        StringBuilder html = new StringBuilder("<html>");
        if (!contraCierreAnterior.isEmpty()) {
            filas(html.append(ENCABEZADO), contraCierreAnterior);
        }
        if (!desdeElSondeoAnterior.isEmpty()) {
            html.append("<b>Desde la lectura de las ").append(HORA.format(instanteAnterior))
                    .append(":</b><br /><br />");
            filas(html, desdeElSondeoAnterior);
        }
        return html.append("</html>").toString();
    }

    private static void filas(StringBuilder html, List<Variacion> variaciones) {
        for (Variacion variacion : variaciones) {
            html.append("<div style='color:")
                    .append(variacion.esSubida() ? "blue" : "red")
                    .append("'>")
                    .append(linea(variacion))
                    .append("</div>");
        }
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
