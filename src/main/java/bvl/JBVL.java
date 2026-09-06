package bvl;

import bvl.schedule.BvlScheduler;
import bvl.schedule.HorarioSondeo;
import bvl.schedule.SondeoListener;
import jakarta.annotation.PostConstruct;

import java.time.Duration;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.beans.factory.annotation.Value;

import javax.swing.*;
import javax.swing.border.BevelBorder;
import javax.swing.border.SoftBevelBorder;
import java.awt.*;
import java.awt.TrayIcon.MessageType;
import java.awt.event.*;
import java.net.URL;

@SuppressWarnings("serial")
@Configurable
public class JBVL extends JFrame {

    private JToggleButton btnIniciar;

    private JButton btnMostrar;

    private JButton btnExportar;

    private JLabel lblHoraInicio;

    private JLabel lblHoraFin;

    private JLabel lblIntervalo;

    private JLabel lblVariacion;

    private JPanel panelContent;

    private JPanel panelNorth;

    private JPanel panelStatusBar;

    private JPanel panelEast;

    private JPanel panelWest;

    private JLabel statusBar;

    private JTextField txtHoraInicio;

    private JTextField txtHoraFin;

    private JTextField txtIntervalo;

    private JTextField txtAlarma;

    private MenuItem hideShowMenuItem;

    private MenuItem exitMenuItem;

    private TrayIcon trayIcon = null;

    private BVL2 bvl;

    private BvlScheduler scheduler;

    @Value("${alarma}")
    private String alarma;

    /**
     * Cuanto aguanta abierta una alerta antes de cerrarse sola. Se configura en
     * {@code deploy/bvl.properties}; el default cubre el caso de que falte, porque
     * {@code --spring.config.location} sustituye al {@code application.properties} empaquetado.
     */
    @Value("${alertaTimeout:00:10:00}")
    private String alertaTimeout;

    /** Alerta en pantalla, si la hay. Solo se toca desde el EDT. */
    private JDialog alertaVisible;

    public JBVL(BVL2 bvl, BvlScheduler scheduler) {
        this.bvl = bvl;
        this.scheduler = scheduler;
        // setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        initComponents();

        if (SystemTray.isSupported()) {
            initTrayIcon();
        } else {
            // disable tray option in your application or
            // perform other actions
        }
        // ...
        // some time later
        // the application state has changed - update the image
        // if (trayIcon != null) {
        // trayIcon.setImage(updatedImage);
        // }
        // ...
        centerAndSize();
    }

    @PostConstruct
    public void init() {
        volcarHorarioEnCampos(scheduler.getHorario());
        txtAlarma.setText(alarma);

        // Enter en cualquiera de los tres campos aplica el horario en caliente.
        ActionListener aplicar = e -> aplicarHorario();
        txtHoraInicio.addActionListener(aplicar);
        txtHoraFin.addActionListener(aplicar);
        txtIntervalo.addActionListener(aplicar);

        scheduler.setListener(new SondeoListener() {
            @Override
            public void onSondeoCompletado() {
                mostrarAlertas();
            }

            @Override
            public void onSondeoFallido(Throwable error) {
                notificarFallo(error);
            }
        });
    }

    private void volcarHorarioEnCampos(HorarioSondeo horario) {
        txtHoraInicio.setText(horario.getInicio().toString());
        txtHoraFin.setText(horario.getFin().toString());
        txtIntervalo.setText(String.format("%02d:%02d:00", horario.getIntervaloMinutos() / 60,
                horario.getIntervaloMinutos() % 60));
    }

    /**
     * Lee los tres campos y reprograma el sondeo sin reiniciar. Si el usuario escribe algo
     * invalido se avisa en la barra de estado y se conserva el horario anterior: mas vale seguir
     * sondeando con la cadencia vieja que quedarse sin sondeo.
     */
    private void aplicarHorario() {
        HorarioSondeo nuevo;
        try {
            nuevo = HorarioSondeo.of(txtHoraInicio.getText(), txtHoraFin.getText(),
                    txtIntervalo.getText());
        } catch (IllegalArgumentException e) {
            statusBar.setText("Horario no aplicado: " + e.getMessage());
            volcarHorarioEnCampos(scheduler.getHorario());
            return;
        }
        scheduler.reprogramar(nuevo);
        volcarHorarioEnCampos(nuevo);
        statusBar.setText((scheduler.isActivo() ? "Sondeando " : "Horario listo ")
                + nuevo.getInicio() + " - " + nuevo.getFin() + ", cada "
                + nuevo.getIntervaloMinutos() + " min");
    }

    /**
     * Presenta el resultado del sondeo. Se invoca desde el hilo del planificador, asi que todo
     * el trabajo de Swing se delega al EDT: el dialogo modal bloqueaba antes el ciclo entero y
     * el intervalo no empezaba a contar hasta que alguien lo cerraba.
     */
    private void mostrarAlertas() {
        final String[] msg;
        try {
            msg = bvl.getVariaciones(Double.parseDouble(txtAlarma.getText()));
        } catch (NumberFormatException e) {
            notificarFallo(e);
            return;
        }
        if (trayIcon != null) {
            trayIcon.displayMessage("Empresas que variaron: " + bvl.getFecha(), msg[0],
                    MessageType.WARNING);
        }
        SwingUtilities.invokeLater(() -> mostrarAlertaConAutoCierre(msg[1]));
    }

    /**
     * Muestra la alerta y la cierra sola pasado {@code alertaTimeout} si nadie la atiende, para
     * que un equipo desatendido no acumule dialogos ni deje uno abierto toda la sesion.
     *
     * <p>Solo hay una alerta en pantalla: al llegar una lectura nueva se descarta la anterior, que
     * ya esta obsoleta. Con intervalo de 5 minutos y timeout de 10 se solaparian dos.
     *
     * <p>Corre en el EDT. El dialogo es modal y abre un bucle de eventos anidado, asi que el
     * {@link Timer} sigue disparando y puede cerrarlo.
     */
    private void mostrarAlertaConAutoCierre(String mensajeHtml) {
        if (alertaVisible != null) {
            alertaVisible.dispose();
            alertaVisible = null;
        }
        long millis;
        try {
            millis = HorarioSondeo.parseDuracion(alertaTimeout, "alertaTimeout").toMillis();
        } catch (IllegalArgumentException e) {
            statusBar.setText("alertaTimeout invalido, se usan 10 min: " + e.getMessage());
            millis = Duration.ofMinutes(10).toMillis();
        }

        JOptionPane panel = new JOptionPane(mensajeHtml, JOptionPane.ERROR_MESSAGE);
        JDialog dialogo = panel.createDialog(JBVL.this, "Alertas");
        alertaVisible = dialogo;

        Timer cierre = new Timer((int) Math.min(millis, Integer.MAX_VALUE), e -> dialogo.dispose());
        cierre.setRepeats(false);
        cierre.start();
        try {
            dialogo.setVisible(true);
        } finally {
            cierre.stop();
            dialogo.dispose();
            if (alertaVisible == dialogo) {
                alertaVisible = null;
            }
        }
    }

    /** Los fallos se avisan por el tray, que no bloquea, en lugar de por un dialogo modal. */
    private void notificarFallo(Throwable error) {
        statusBar.setText("Error: " + error.getMessage());
        if (trayIcon != null) {
            trayIcon.displayMessage("Fallo el sondeo", String.valueOf(error.getMessage()),
                    MessageType.ERROR);
        }
    }

    public void centerAndSize() {
        setSize(280, 290);

        Toolkit toolkit = Toolkit.getDefaultToolkit();
        Dimension screenSize = toolkit.getScreenSize();

        // Calculate the frame location
        int x = (screenSize.width - getWidth()) / 2;
        int y = (screenSize.height - getHeight()) / 2;
        setLocation(x, y);
    }

    private void initTrayIcon() {
        // get the SystemTray instance
        SystemTray tray = SystemTray.getSystemTray();
        // load an image
        Image image = null;

        URL imgUrl = getClass().getResource("/res/bvl.gif");
        image = Toolkit.getDefaultToolkit().getImage(imgUrl);

        // create a action listener to listen for default action executed on
        // the tray icon
        ActionListener listener = new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                System.exit(0);
            }
        };
        // create a popup menu
        PopupMenu popup = new PopupMenu();
        // create menu item for the default action
        exitMenuItem = new MenuItem("Cerrar");
        hideShowMenuItem = new MenuItem("Ocultar");

        hideShowMenuItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (hideShowMenuItem.getLabel().equals("Ocultar")) {
                    JBVL.this.setVisible(false);
                    hideShowMenuItem.setLabel("Mostrar");
                } else {
                    JBVL.this.setVisible(true);
                    hideShowMenuItem.setLabel("Ocultar");
                }
            }
        });
        exitMenuItem.addActionListener(listener);
        popup.add(hideShowMenuItem);
        popup.add(exitMenuItem);

        // / ... add other items
        // construct a TrayIcon
        trayIcon = new TrayIcon(image, "BVL", popup);
        trayIcon.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                System.err.println(e);
                trayIcon.displayMessage("CAPTION", "TEXT", MessageType.WARNING);
            }
        });
        // set the TrayIcon properties
        // trayIcon.addActionListener(listener);
        // ...
        // add the tray image
        try {
            tray.add(trayIcon);
        } catch (AWTException e) {
            System.err.println(e);
        }
        // ...
    }

    private void initComponents() {// GEN-BEGIN:initComponents
        panelContent = new JPanel();
        lblHoraInicio = new JLabel();
        txtHoraInicio = new JTextField();
        lblHoraFin = new JLabel();
        txtHoraFin = new JTextField();
        lblIntervalo = new JLabel();
        txtIntervalo = new JTextField();
        lblVariacion = new JLabel();
        txtAlarma = new JTextField();
        btnIniciar = new JToggleButton();
        btnMostrar = new JButton();
        btnExportar = new JButton("Exportar");
        panelNorth = new JPanel();
        panelStatusBar = new JPanel();
        statusBar = new JLabel();
        panelEast = new JPanel();
        panelWest = new JPanel();

        getContentPane().setLayout(new java.awt.BorderLayout(0, 10));

        setTitle("BVL");
        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosing(java.awt.event.WindowEvent evt) {
                exitForm(evt);
            }
        });

        panelContent.setLayout(new java.awt.GridLayout(6, 2, 10, 10));

        lblHoraInicio.setText("Hora de Inicio");
        panelContent.add(lblHoraInicio);

        panelContent.add(txtHoraInicio);

        lblHoraFin.setText("Hora de Fin");
        panelContent.add(lblHoraFin);

        panelContent.add(txtHoraFin);

        lblIntervalo.setText("Intervalo");
        panelContent.add(lblIntervalo);

        panelContent.add(txtIntervalo);

        lblVariacion.setText("Variación (%):");
        panelContent.add(lblVariacion);

        panelContent.add(txtAlarma);

        // btnIniciar.setEnabled(false);
        btnIniciar.setText("Iniciar");
        btnIniciar.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                if (btnIniciar.isSelected()) {
                    aplicarHorario();
                    scheduler.iniciar();
                    btnIniciar.setText("Detener");
                    statusBar.setText("Sondeando " + scheduler.getHorario().getInicio() + " - "
                            + scheduler.getHorario().getFin());
                } else {
                    scheduler.detener();
                    btnIniciar.setText("Iniciar");
                    statusBar.setText("Detenido");
                }
            }
        });

        panelContent.add(btnMostrar);
        // btnMostrar.setEnabled(false);
        btnMostrar.setText("Mostrar");
        final JDisplayData dlgData = new JDisplayData();
        btnMostrar.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                dlgData.setVisible(true);
            }
        });

        panelContent.add(btnIniciar);

        panelContent.add(btnExportar);

        btnExportar.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                // new ExportDialog().setVisible(true);
            }
        });

        getContentPane().add(panelContent, java.awt.BorderLayout.CENTER);

        getContentPane().add(panelNorth, java.awt.BorderLayout.NORTH);

        panelStatusBar.setLayout(new java.awt.BorderLayout());

        statusBar.setText("Listo");
        statusBar.setBorder(new SoftBevelBorder(BevelBorder.LOWERED));
        panelStatusBar.add(statusBar, java.awt.BorderLayout.CENTER);

        getContentPane().add(panelStatusBar, java.awt.BorderLayout.SOUTH);

        getContentPane().add(panelEast, java.awt.BorderLayout.EAST);

        getContentPane().add(panelWest, java.awt.BorderLayout.WEST);

        pack();
    }// GEN-END:initComponents

    protected void exitForm(WindowEvent evt) {
        // TODO Auto-generated method stub

    }

    @Override
    public void setVisible(boolean b) {
        if (hideShowMenuItem != null) {
            hideShowMenuItem.setLabel(b ? "Ocultar" : "Mostrar");
        }
        super.setVisible(b);
    }

}
