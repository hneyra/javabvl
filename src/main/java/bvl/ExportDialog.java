package bvl;

import bvl.service.BvlService;

import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Calendar;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;

@SuppressWarnings("serial")
public class ExportDialog extends JDialog {

  private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

  private JTextField txtFrom;
  private JTextField txtTo;
  private JPanel panel;
  private JButton btnExportar;
  private BvlService bvlService;

  public ExportDialog(BvlService bvlService) {
    this.bvlService = bvlService;
    initComponents();
    centerAndSize();
    setAlwaysOnTop(true);
    setTitle("Exportar a Microsoft Excel");
  }

  public void centerAndSize() {
    setSize(250, 150);

    Toolkit toolkit = Toolkit.getDefaultToolkit();
    Dimension screenSize = toolkit.getScreenSize();

    // Calculate the frame location
    int x = (screenSize.width - getWidth()) / 2;
    int y = (screenSize.height - getHeight()) / 2;
    setLocation(x, y);
  }

  public void initComponents() {
    Calendar cal = Calendar.getInstance();
    cal.set(Calendar.HOUR_OF_DAY, 8);
    txtFrom = new JTextField(BvlService.sdfd.format(cal.getTime()));
    cal.set(Calendar.HOUR_OF_DAY, 17);
    txtTo = new JTextField(BvlService.sdfd.format(cal.getTime()));
    btnExportar = new JButton("Exportar");
    panel = new JPanel(new GridLayout(3, 2, 10, 10));
    panel.setBorder(new EmptyBorder(10, 10, 10, 10));
    setContentPane(panel);
    panel.add(new JLabel("Desde"));
    panel.add(txtFrom);
    panel.add(new JLabel("Hasta"));
    panel.add(txtTo);
    panel.add(btnExportar);

    btnExportar.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        exportar();
      }
    });
  }

  private void exportar() {
    bvlService.exportar(LocalDateTime.parse(txtFrom.getText(), formatter),
        LocalDateTime.parse(txtTo.getText(), formatter));
  }
}
