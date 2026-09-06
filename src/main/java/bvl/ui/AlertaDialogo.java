package bvl.ui;

import java.awt.Component;
import java.time.Duration;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.Timer;

/**
 * El aviso de variaciones, que se cierra solo si nadie lo atiende.
 *
 * <p>Dos reglas, las dos por el mismo motivo (un equipo desatendido durante toda la sesion):
 * <ul>
 *   <li>la alerta se cierra sola pasado el timeout, para no dejar un dialogo abierto todo el dia;
 *   <li>solo hay una en pantalla: al llegar una lectura nueva se descarta la anterior, que ya esta
 *       obsoleta. Con intervalo de 5 minutos y timeout de 10 se solaparian dos.
 * </ul>
 *
 * <p>Se usa <b>solo desde el EDT</b>. El dialogo es modal y abre un bucle de eventos anidado, asi
 * que el {@link Timer} sigue disparando y puede cerrarlo.
 */
public class AlertaDialogo {

    private static final String TITULO = "Alertas";

    private final Component padre;

    /** La alerta en pantalla, si la hay. Solo se toca desde el EDT. */
    private JDialog visible;

    public AlertaDialogo(Component padre) {
        this.padre = padre;
    }

    public void mostrar(String mensajeHtml, Duration timeout) {
        descartarLaAnterior();

        JOptionPane panel = new JOptionPane(mensajeHtml, JOptionPane.ERROR_MESSAGE);
        JDialog dialogo = panel.createDialog(padre, TITULO);
        visible = dialogo;

        int millis = (int) Math.min(timeout.toMillis(), Integer.MAX_VALUE);
        Timer cierre = new Timer(millis, e -> dialogo.dispose());
        cierre.setRepeats(false);
        cierre.start();
        try {
            // Bloquea aqui hasta que el usuario cierra o salta el temporizador.
            dialogo.setVisible(true);
        } finally {
            cierre.stop();
            dialogo.dispose();
            if (visible == dialogo) {
                visible = null;
            }
        }
    }

    private void descartarLaAnterior() {
        if (visible != null) {
            visible.dispose();
            visible = null;
        }
    }
}
