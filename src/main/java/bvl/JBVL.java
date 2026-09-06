package bvl;

import bvl.schedule.BvlScheduler;
import bvl.schedule.HorarioSondeo;
import bvl.schedule.SondeoListener;
import jakarta.annotation.PostConstruct;
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

    private JLabel lblIntervalo;

    private JLabel lblVariacion;

    private JPanel panelContent;

    private JPanel panelNorth;

    private JPanel panelStatusBar;

    private JPanel panelEast;

    private JPanel panelWest;

    private JLabel statusBar;

    private JTextField txtHoraInicio;

    private JTextField txtIntervalo;

    private JTextField txtAlarma;

    private MenuItem hideShowMenuItem;

    private MenuItem exitMenuItem;

    private TrayIcon trayIcon = null;

    private BVL2 bvl;

    private BvlScheduler scheduler;

    @Value("${alarma}")
    private String alarma;

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
        HorarioSondeo horario = scheduler.getHorario();
        // Solo informativos: la planificacion la manda el cron, no estos campos. Se dejan no
        // editables para que la ventana no aparente controlar algo que ya no controla.
        txtHoraInicio.setText(horario.getInicio() + " - " + horario.getFin());
        txtHoraInicio.setEditable(false);
        txtIntervalo.setText("cada " + horario.getIntervaloMinutos() + " min (L-V)");
        txtIntervalo.setEditable(false);
        txtAlarma.setText(alarma);

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
        SwingUtilities.invokeLater(
                () -> JOptionPane.showMessageDialog(JBVL.this, msg[1], "Alertas", 0));
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
        setSize(250, 250);

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

        panelContent.setLayout(new java.awt.GridLayout(5, 2, 10, 10));

        lblHoraInicio.setText("Hora de Inicio");
        panelContent.add(lblHoraInicio);

        panelContent.add(txtHoraInicio);

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
