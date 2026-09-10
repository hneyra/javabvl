package bvl.ui;

import bvl.alert.AlertaFormatter;
import bvl.alert.DetectorVariaciones;
import bvl.alert.Variacion;
import bvl.config.BvlProperties;
import bvl.schedule.BvlScheduler;
import bvl.schedule.HorarioSondeo;
import bvl.schedule.Lectura;
import bvl.schedule.ResultadoSondeo;
import bvl.schedule.SondeoListener;
import jakarta.annotation.PostConstruct;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.border.BevelBorder;
import javax.swing.border.SoftBevelBorder;

/**
 * La ventana de la aplicacion: compone la interfaz y traduce lo que hace el usuario en ordenes al
 * planificador.
 *
 * <p>Reparto de papeles: {@link PanelSondeo} es el formulario, {@link BandejaSistema} los avisos,
 * {@link AlertaDialogo} el aviso de variaciones. Aqui solo queda el pegamento.
 *
 * <p><b>Hilos.</b> Los avisos de {@link SondeoListener} llegan en el hilo del planificador, que no
 * puede tocar Swing. Todo lo que hacen {@link #onSondeoCompletado} y {@link #onSondeoFallido} se
 * despacha al EDT; ese hilo no puede quedarse esperando, porque mientras tanto no hay sondeos.
 *
 * <p>La ventana no se cierra al pulsar la X: se oculta (comportamiento por defecto de
 * {@link JFrame}) y la aplicacion sigue viva en la bandeja, que es de donde se sale con Cerrar.
 */
public class VentanaPrincipal extends JFrame implements PanelSondeo.Acciones, SondeoListener {

    private static final int ANCHO = 280;
    private static final int ALTO = 290;

    private final BvlScheduler scheduler;
    private final DetectorVariaciones detector;
    private final AlertaFormatter formatter;
    private final BvlProperties properties;

    private final PanelSondeo panel;
    private final JLabel barraEstado = new JLabel("Listo");
    private final AlertaDialogo alerta;
    private final VentanaDatos ventanaDatos = new VentanaDatos();
    private final Notificador notificador;

    public VentanaPrincipal(BvlScheduler scheduler, DetectorVariaciones detector,
                            AlertaFormatter formatter, BvlProperties properties) {
        super("BVL");
        this.scheduler = scheduler;
        this.detector = detector;
        this.formatter = formatter;
        this.properties = properties;
        this.panel = new PanelSondeo(this);
        this.alerta = new AlertaDialogo(this);

        componer();
        this.notificador = BandejaSistema.instalar(this);

        panel.mostrarHorario(scheduler.getHorario());
        panel.mostrarAlarma(properties.getAlarma());
        centrarYDimensionar();
    }

    /** Se registra despues de construir, para no dejar escapar {@code this} a medio construir. */
    @PostConstruct
    public void registrarseEnElPlanificador() {
        scheduler.setListener(this);
    }

    private void componer() {
        JPanel contenedorEstado = new JPanel(new BorderLayout());
        barraEstado.setBorder(new SoftBevelBorder(BevelBorder.LOWERED));
        contenedorEstado.add(barraEstado, BorderLayout.CENTER);

        setLayout(new BorderLayout(0, 10));
        add(panel, BorderLayout.CENTER);
        add(contenedorEstado, BorderLayout.SOUTH);
        // Paneles vacios: hacen de margen alrededor del formulario.
        add(new JPanel(), BorderLayout.NORTH);
        add(new JPanel(), BorderLayout.EAST);
        add(new JPanel(), BorderLayout.WEST);
        pack();
    }

    private void centrarYDimensionar() {
        setSize(ANCHO, ALTO);
        Dimension pantalla = Toolkit.getDefaultToolkit().getScreenSize();
        setLocation((pantalla.width - getWidth()) / 2, (pantalla.height - getHeight()) / 2);
    }

    // --- Acciones del formulario ---------------------------------------------------------------

    /** Enter en un campo de horario: se aplica en caliente, sin reiniciar. */
    @Override
    public void aplicarHorario() {
        aplicar();
    }

    /**
     * Aplica el horario tecleado. Si no vale, avisa del motivo y deja el texto como esta, para que
     * se pueda corregir; el horario vigente no cambia.
     *
     * <p>Antes se reponia el horario anterior en los campos y el motivo se escribia en la barra de
     * estado, donde lo tapaba el siguiente mensaje: parecia que la ventana volvia sola a 9:40.
     *
     * @return si el horario se aplico
     */
    private boolean aplicar() {
        HorarioSondeo nuevo;
        try {
            nuevo = panel.leerHorario();
        } catch (IllegalArgumentException e) {
            estado("Horario no aplicado");
            // Modal a proposito: responde a un clic o a un Enter del usuario, en el EDT. La regla
            // de no abrir dialogos va por el hilo del planificador, que no debe quedarse esperando.
            JOptionPane.showMessageDialog(this,
                    e.getMessage() + "\n\nSigue vigente: " + describir(scheduler.getHorario()) + ".",
                    "Horario no válido", JOptionPane.WARNING_MESSAGE);
            return false;
        }
        scheduler.reprogramar(nuevo);
        panel.mostrarHorario(nuevo);
        estado((scheduler.isActivo() ? "Sondeando " : "Horario listo ") + describir(nuevo));
        return true;
    }

    @Override
    public void iniciar() {
        if (!aplicar()) {
            // Arrancar con un horario que el usuario no ha pedido es peor que no arrancar.
            panel.marcarSondeando(false);
            return;
        }
        scheduler.iniciar();
        panel.marcarSondeando(true);
        estado("Sondeando " + describir(scheduler.getHorario()));
    }

    private static String describir(HorarioSondeo horario) {
        return horario.getInicio() + " - " + horario.getFin() + ", cada "
                + horario.getIntervaloMinutos() + " min";
    }

    @Override
    public void detener() {
        scheduler.detener();
        panel.marcarSondeando(false);
        estado("Detenido");
    }

    @Override
    public void mostrarDatos() {
        ventanaDatos.setVisible(true);
    }

    @Override
    public void exportar() {
        // Sin efecto, como hasta ahora. El boton existe desde el diseño original y la exportacion
        // a demanda nunca se llego a implementar; se conserva para no cambiar la ventana.
    }

    // --- Avisos del planificador ---------------------------------------------------------------

    @Override
    public void onSondeoCompletado(ResultadoSondeo resultado) {
        SwingUtilities.invokeLater(() -> presentar(resultado));
    }

    @Override
    public void onSondeoFallido(Throwable error) {
        SwingUtilities.invokeLater(() -> notificarFallo(error));
    }

    /**
     * Presenta las variaciones de la lectura recien hecha. El aviso lleva dos bloques: lo que se ha
     * movido respecto al cierre de ayer, que publica la BVL, y —si hay lectura previa en esta
     * sesion— lo que se ha movido desde el sondeo anterior.
     *
     * <p>El umbral es el mismo para los dos, el que hay escrito en la ventana.
     */
    private void presentar(ResultadoSondeo resultado) {
        double umbral;
        try {
            umbral = panel.leerAlarma();
        } catch (NumberFormatException e) {
            notificarFallo(e);
            return;
        }
        Lectura actual = resultado.actual();
        List<Variacion> contraCierre = detector.detectar(actual.items(), umbral);

        List<Variacion> intradia = List.of();
        LocalDateTime instanteAnterior = null;
        if (resultado.hayConQueComparar()) {
            instanteAnterior = resultado.previa().fecha();
            intradia = detector.detectarDesde(resultado.previa(), actual, umbral);
        }

        notificador.aviso("Empresas que variaron: " + actual.fecha(),
                formatter.texto(contraCierre, intradia, instanteAnterior));
        alerta.mostrar(formatter.html(contraCierre, intradia, instanteAnterior), timeoutAlerta());
    }

    /** Los fallos se avisan por la bandeja, que no bloquea, en lugar de por un dialogo modal. */
    private void notificarFallo(Throwable error) {
        estado("Error: " + error.getMessage());
        notificador.error("Fallo el sondeo", String.valueOf(error.getMessage()));
    }

    /**
     * El timeout se interpreta aqui y no al arrancar: si esta mal escrito se avisa y se sigue con
     * el valor por defecto, en vez de impedir que la aplicacion arranque.
     */
    private Duration timeoutAlerta() {
        try {
            return HorarioSondeo.parseDuracion(properties.getAlertaTimeout(), "alertaTimeout");
        } catch (IllegalArgumentException e) {
            estado("alertaTimeout invalido, se usan 10 min: " + e.getMessage());
            return BvlProperties.alertaTimeoutPorDefecto();
        }
    }

    private void estado(String mensaje) {
        barraEstado.setText(mensaje);
    }

    @Override
    public void setVisible(boolean visible) {
        if (notificador != null) {
            notificador.sincronizarVisibilidad(visible);
        }
        super.setVisible(visible);
    }
}
