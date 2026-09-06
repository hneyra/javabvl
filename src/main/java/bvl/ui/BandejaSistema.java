package bvl.ui;

import java.awt.AWTException;
import java.awt.Image;
import java.awt.MenuItem;
import java.awt.PopupMenu;
import java.awt.SystemTray;
import java.awt.Toolkit;
import java.awt.TrayIcon;
import java.awt.TrayIcon.MessageType;
import java.net.URL;
import javax.swing.JFrame;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Icono en la bandeja del sistema, con menu para ocultar la ventana y para cerrar la aplicacion.
 *
 * <p>Es la unica via de aviso que no interrumpe: la aplicacion pasa el dia en segundo plano y el
 * globo de la bandeja aparece y se va solo.
 */
public final class BandejaSistema implements Notificador {

    private static final Logger logger = LoggerFactory.getLogger(BandejaSistema.class);

    private static final String ICONO = "/res/bvl.gif";
    private static final String OCULTAR = "Ocultar";
    private static final String MOSTRAR = "Mostrar";

    private final TrayIcon trayIcon;
    private final MenuItem itemVisibilidad;

    private BandejaSistema(TrayIcon trayIcon, MenuItem itemVisibilidad) {
        this.trayIcon = trayIcon;
        this.itemVisibilidad = itemVisibilidad;
    }

    /**
     * Instala el icono para esta ventana.
     *
     * @return un {@link Notificador} real, o {@link Notificador#NINGUNO} si el escritorio no
     *         soporta bandeja o el sistema rechaza el icono. La aplicacion sigue funcionando: solo
     *         pierde los avisos.
     */
    public static Notificador instalar(JFrame ventana) {
        if (!SystemTray.isSupported()) {
            logger.info("El escritorio no soporta bandeja del sistema: no habra avisos emergentes");
            return Notificador.NINGUNO;
        }

        MenuItem itemVisibilidad = new MenuItem(OCULTAR);
        itemVisibilidad.addActionListener(e -> ventana.setVisible(!ventana.isVisible()));

        MenuItem itemCerrar = new MenuItem("Cerrar");
        itemCerrar.addActionListener(e -> System.exit(0));

        PopupMenu menu = new PopupMenu();
        menu.add(itemVisibilidad);
        menu.add(itemCerrar);

        URL urlIcono = BandejaSistema.class.getResource(ICONO);
        Image imagen = Toolkit.getDefaultToolkit().getImage(urlIcono);
        TrayIcon trayIcon = new TrayIcon(imagen, "BVL", menu);

        try {
            SystemTray.getSystemTray().add(trayIcon);
        } catch (AWTException e) {
            logger.error("No se pudo instalar el icono en la bandeja: {}", e.getMessage(), e);
            return Notificador.NINGUNO;
        }
        return new BandejaSistema(trayIcon, itemVisibilidad);
    }

    @Override
    public void aviso(String titulo, String mensaje) {
        trayIcon.displayMessage(titulo, mensaje, MessageType.WARNING);
    }

    @Override
    public void error(String titulo, String mensaje) {
        trayIcon.displayMessage(titulo, mensaje, MessageType.ERROR);
    }

    @Override
    public void sincronizarVisibilidad(boolean visible) {
        itemVisibilidad.setLabel(visible ? OCULTAR : MOSTRAR);
    }
}
