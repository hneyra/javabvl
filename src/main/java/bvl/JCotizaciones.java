package bvl;

import bvl.domain.Item;
import bvl.service.BvlService;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;
import java.util.Map;

@SuppressWarnings("serial")
public class JCotizaciones extends JPanel {

    private JTextField txtFecha;
    private JComboBox cmbHora;
    private JLabel lblHora;
    private JLabel lblFecha;
    private JTextField filterText;
    private JTableAcciones tblAcciones;

    private BvlService conn;
    private JButton btnFilter;
    private JButton btnExport;
    private XlsWriter xls;
    private File file;

    public JCotizaciones(BvlService conn) {
        this.conn = conn;
        initComponents();
    }

    private void initComponents() {
        setLayout(new BorderLayout());
        JPanel panelNorth = new JPanel();
        lblFecha = new JLabel("Fecha");
        lblHora = new JLabel("Hora");
        txtFecha = new JTextField(10);
        cmbHora = new JComboBox();
        filterText = new JTextField(10);
        btnFilter = new JButton("Modificar Filtro");
        tblAcciones = new JTableAcciones(filterText);
        btnExport = new JButton("Exportar a Excel");

        txtFecha.setText(BvlService.sdfd.format(new Date()));
        txtFecha.addActionListener(new FechaActionListener());
        cmbHora.addActionListener(new HoraActionListener());

        panelNorth.add(lblFecha);
        panelNorth.add(txtFecha);
        panelNorth.add(lblHora);
        panelNorth.add(cmbHora);
        panelNorth.add(filterText);
        // panelNorth.add(btnFilter);
        // panelNorth.add(btnExport);

        btnFilter.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                new JFilterDialog().setVisible(true);
            }
        });

        btnExport.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                // createXLS();
            }
        });

        add(panelNorth, BorderLayout.NORTH);
        add(new JScrollPane(tblAcciones), BorderLayout.CENTER);
    }

    class FechaActionListener implements ActionListener {

        @Override
        public void actionPerformed(ActionEvent e) {
            Map<Long, LocalDateTime> map = conn.getHoras(LocalDateTime.parse(txtFecha.getText()));
            cmbHora.removeAllItems();
            for (Long key : map.keySet()) {
                cmbHora.addItem(map.get(key));
                cmbHora.setEnabled(true);
            }
        }
    }

    class HoraActionListener implements ActionListener {

        @Override
        public void actionPerformed(ActionEvent e) {
            if (cmbHora.getSelectedItem() == null) {
                return;
            }
            LocalDateTime fecha = LocalDateTime
                    .parse(txtFecha.getText() + " " + cmbHora.getSelectedItem());
            List<Item> ans = conn.getItems(fecha);
            String[] titles = {"ACCIONES", "ANT", "A.FEC", "APE", "EX-DER", "ULT", "COM", "VEN",
                    "No.ACC.",
                    "MONTO NEGOCIADO", "No.OPE.", "MONEDA", "VARIACION"};
            tblAcciones.setFilterText(filterText);
            // TODO: comentado
            // tblAcciones.setModel(new AccionesTableModel(ans, new
            // Vector(Arrays.asList(titles))));
        }
    }
}
