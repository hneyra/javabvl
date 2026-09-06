package bvl;

import java.awt.AWTException;
import java.awt.Dimension;
import java.awt.Image;
import java.awt.MenuItem;
import java.awt.PopupMenu;
import java.awt.SystemTray;
import java.awt.Toolkit;
import java.awt.TrayIcon;
import java.awt.TrayIcon.MessageType;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowEvent;
import java.net.URL;
import java.util.StringTokenizer;
import jakarta.annotation.PostConstruct;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.border.BevelBorder;
import javax.swing.border.SoftBevelBorder;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.beans.factory.annotation.Value;

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

  @Value("${horaInicio}")
  private String horaInicio;

  @Value("${intervalo}")
  private String intervalo;

  @Value("${alarma}")
  private String alarma;

  public JBVL(BVL2 bvl) {
    this.bvl = bvl;
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
    txtHoraInicio.setText(horaInicio);
    txtIntervalo.setText(intervalo);
    txtAlarma.setText(alarma);
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
        btnIniciar.setEnabled(!btnIniciar.isEnabled());
        new Thread() {
          @Override
          public void run() {
            long tiempo = parseTime(txtIntervalo.getText());
            while (true) {
              try {
                bvl.process();
                Double alarma = Double.parseDouble(txtAlarma.getText());
                String msg[] = bvl.getVariaciones(alarma);
                trayIcon.displayMessage("Empresas que variaron: " + bvl.getFecha(), msg[0],
                    MessageType.WARNING);
                JOptionPane.showMessageDialog(JBVL.this, msg[1], "Alertas", 0);
                sleep(1000 * tiempo);
              } catch (InterruptedException e) {
                e.printStackTrace();
              }
            }
          }
        }.start();
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

  /**
   * Encuentra el total de segundos de contenidos en una hora hh:mm:ss por ejemplo 2:15:23 = 2*3600
   * + 15*60 + 23
   */
  public static int parseTime(String time) {
    StringTokenizer strTk = new StringTokenizer(time, ":");
    int ans = 0, i = 2;

    while (strTk.hasMoreElements()) {
      ans += Math.pow(60, i--) * Integer.parseInt(strTk.nextElement().toString());
    }
    // System.out.println(time + "=" + ans);

    return ans;
  }

  @Override
  public void setVisible(boolean b) {
    if (hideShowMenuItem != null) {
      hideShowMenuItem.setLabel(b ? "Ocultar" : "Mostrar");
    }
    super.setVisible(b);
  }

}
